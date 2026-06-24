package com.vitalloop.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.vitalloop.contracts.VitalsReadingReceived;
import com.vitalloop.triage.domain.RecommendedAction;
import com.vitalloop.triage.domain.Severity;
import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import com.vitalloop.triage.port.TriageResultHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end test of the publish→consume path against a real Pub/Sub emulator started by
 * Testcontainers.
 *
 * <p>It boots the full application under the {@code local} profile (so the real {@code
 * PubSubLocalInitializer} and {@code PubSubSubscriberInitializer} run), publishes a {@link
 * VitalsReadingReceived} event to the topic, and asserts the consumer drives the triage engine to
 * the expected decision — correlated by {@code eventId}.
 *
 * <p><b>Gating:</b> tagged {@code integration} so it is excluded from the default (Docker-free)
 * build via Surefire {@code excludedGroups}; it runs only via {@code mvn -Pintegration ...}.
 *
 * <p><b>Regression guard for the startup-ordering fix:</b> the topic and subscription are created
 * by {@code PubSubLocalInitializer} on {@code ApplicationStartedEvent}, which must fire
 * <i>before</i> {@code PubSubSubscriberInitializer} attaches the subscriber on {@code
 * ApplicationReadyEvent}. If that ordering regresses, the subscriber attaches to a missing
 * subscription, dies permanently with NOT_FOUND, and no decision is ever produced — failing the
 * assertions below. The explicit {@code getSubscription}/{@code getTopic} checks make that contract
 * visible.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
@ActiveProfiles("local")
@Import(PubSubEndToEndIntegrationTest.CapturingConfig.class)
class PubSubEndToEndIntegrationTest {

  private static final int EMULATOR_PORT = 8085;
  private static final String PROJECT_ID = "vitalloop-local";

  // Pinned for reproducibility. MUST stay in sync with the pubsub-emulator image
  // in docker-compose.yml.
  private static final DockerImageName EMULATOR_IMAGE =
      DockerImageName.parse(
          "gcr.io/google.com/cloudsdktool/google-cloud-cli:574.0.0"
              + "@sha256:12d42ee2c22a4ed7d551654c88b04648626319cd3999117c6b32b166bbe5cbc8");

  @Container
  static final GenericContainer<?> PUBSUB =
      new GenericContainer<>(EMULATOR_IMAGE)
          .withExposedPorts(EMULATOR_PORT)
          .withCommand(
              "/bin/sh",
              "-c",
              "gcloud beta emulators pubsub start --project="
                  + PROJECT_ID
                  + " --host-port=0.0.0.0:"
                  + EMULATOR_PORT)
          .waitingFor(
              Wait.forLogMessage(".*Server started, listening on.*", 1)
                  .withStartupTimeout(Duration.ofSeconds(120)));

  @DynamicPropertySource
  static void pubSubProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.gcp.pubsub.emulator-host",
        () -> PUBSUB.getHost() + ":" + PUBSUB.getMappedPort(EMULATOR_PORT));
    registry.add("spring.cloud.gcp.project-id", () -> PROJECT_ID);
  }

  @Autowired private PubSubTemplate pubSubTemplate;
  @Autowired private PubSubAdmin pubSubAdmin;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CapturingTriageResultHandler captured;

  @Value("${vitalloop.pubsub.topic}")
  private String topic;

  @Value("${vitalloop.pubsub.subscription}")
  private String subscription;

  @Test
  void publishesAndConsumesAcrossTheEmulator() throws Exception {
    // Regression guard for the startup-ordering fix: the subscription (and topic) must already
    // exist, created on ApplicationStartedEvent before the subscriber attached on
    // ApplicationReadyEvent. If this regressed, the subscriber would have died with NOT_FOUND and
    // the awaits below would time out.
    assertThat(pubSubAdmin.getTopic(topic)).as("topic created before subscribe").isNotNull();
    assertThat(pubSubAdmin.getSubscription(subscription))
        .as("subscription created before subscribe")
        .isNotNull();

    // CRITICAL-range reading -> PHYSICIAN_ESCALATION.
    String criticalEventId = publish("patient-crit", "blood_glucose", 268.0, "mg/dL");
    TriageDecision criticalDecision = awaitDecision(criticalEventId, Duration.ofSeconds(10));
    assertThat(criticalDecision)
        .as("decision for critical eventId %s", criticalEventId)
        .isNotNull();
    assertThat(criticalDecision.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(criticalDecision.recommendedAction())
        .isEqualTo(RecommendedAction.PHYSICIAN_ESCALATION);

    // NORMAL-range reading -> NONE.
    String normalEventId = publish("patient-norm", "blood_glucose", 95.0, "mg/dL");
    TriageDecision normalDecision = awaitDecision(normalEventId, Duration.ofSeconds(10));
    assertThat(normalDecision).as("decision for normal eventId %s", normalEventId).isNotNull();
    assertThat(normalDecision.severity()).isEqualTo(Severity.NORMAL);
    assertThat(normalDecision.recommendedAction()).isEqualTo(RecommendedAction.NONE);
  }

  private String publish(String patientId, String vitalType, double value, String unit)
      throws Exception {
    String eventId = UUID.randomUUID().toString();
    VitalsReadingReceived event =
        new VitalsReadingReceived(
            eventId, patientId, vitalType, value, unit, Instant.now(), Instant.now());
    pubSubTemplate.publish(topic, objectMapper.writeValueAsString(event)).get(5, TimeUnit.SECONDS);
    return eventId;
  }

  private TriageDecision awaitDecision(String eventId, Duration timeout) {
    long deadlineNanos = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadlineNanos) {
      TriageDecision decision = captured.byEventId.get(eventId);
      if (decision != null) {
        return decision;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    return captured.byEventId.get(eventId);
  }

  /** Replaces the logging handler with one that records decisions keyed by the event id. */
  @TestConfiguration
  static class CapturingConfig {
    @Bean
    @Primary
    CapturingTriageResultHandler capturingTriageResultHandler() {
      return new CapturingTriageResultHandler();
    }
  }

  /**
   * Captures decisions keyed by {@code eventId}, which it reads from the MDC the inbound consumer
   * sets before invoking the pipeline — exercising (and depending on) that correlation wiring.
   */
  static class CapturingTriageResultHandler implements TriageResultHandler {
    final Map<String, TriageDecision> byEventId = new ConcurrentHashMap<>();

    @Override
    public void handle(VitalsReading reading, TriageDecision decision) {
      String eventId = MDC.get("eventId");
      if (eventId != null) {
        byEventId.put(eventId, decision);
      }
    }
  }
}
