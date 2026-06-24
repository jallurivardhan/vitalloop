package com.vitalloop.ingestion.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.vitalloop.contracts.VitalsReadingReceived;
import com.vitalloop.ingestion.port.EventPublisher;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Publishes a real event to a Pub/Sub backend (the local emulator).
 *
 * <p>Gated two ways so it never runs in CI or without an emulator:
 *
 * <ul>
 *   <li>{@code @Tag("integration")} — excluded by Surefire's {@code excludedGroups} by default.
 *   <li>{@code @EnabledIfEnvironmentVariable} — skipped unless {@code PUBSUB_EMULATOR_HOST} is set.
 * </ul>
 *
 * Run with {@code make dev-up} and {@code -DexcludedGroups= -Dgroups=integration}.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "PUBSUB_EMULATOR_HOST", matches = ".+")
@SpringBootTest
class PubSubEventPublisherIntegrationTest {

  @Autowired private EventPublisher eventPublisher;
  @Autowired private PubSubAdmin pubSubAdmin;

  @Value("${vitalloop.pubsub.topic}")
  private String topic;

  @Test
  void publishesEventToEmulator() {
    if (pubSubAdmin.getTopic(topic) == null) {
      pubSubAdmin.createTopic(topic);
    }

    VitalsReadingReceived event =
        new VitalsReadingReceived(
            "evt-it-1", "patient-1", "heart_rate", 72.0, "bpm", Instant.now(), Instant.now());

    assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();
  }
}
