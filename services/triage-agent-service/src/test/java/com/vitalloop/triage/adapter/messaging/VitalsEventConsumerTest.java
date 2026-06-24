package com.vitalloop.triage.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.vitalloop.contracts.VitalsReadingReceived;
import com.vitalloop.triage.application.TriageService;
import com.vitalloop.triage.domain.RecommendedAction;
import com.vitalloop.triage.domain.Severity;
import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import com.vitalloop.triage.port.TriageEngine;
import com.vitalloop.triage.port.TriageResultHandler;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Verifies the inbound consumer drives the full chain: a received event triggers the (mocked)
 * triage engine and the (mocked) result handler, and the message is acknowledged.
 */
class VitalsEventConsumerTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder().addModule(new JavaTimeModule()).build();

  /** Minimal in-memory acknowledgeable message wrapping a JSON payload. */
  private static final class FakeMessage implements BasicAcknowledgeablePubsubMessage {
    private final PubsubMessage message;
    boolean acked;
    boolean nacked;

    FakeMessage(String json) {
      this.message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(json)).build();
    }

    @Override
    public ProjectSubscriptionName getProjectSubscriptionName() {
      return ProjectSubscriptionName.of("vitalloop-local", "vitals-reading-received-sub");
    }

    @Override
    public PubsubMessage getPubsubMessage() {
      return message;
    }

    @Override
    public CompletableFuture<Void> ack() {
      acked = true;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> nack() {
      nacked = true;
      return CompletableFuture.completedFuture(null);
    }
  }

  @Test
  void receivedEventInvokesEngineAndHandlerAndAcks() throws Exception {
    TriageEngine engine = Mockito.mock(TriageEngine.class);
    TriageResultHandler handler = Mockito.mock(TriageResultHandler.class);
    when(engine.triage(any()))
        .thenReturn(
            new TriageDecision(Severity.CRITICAL, RecommendedAction.PHYSICIAN_ESCALATION, "low"));

    TriageService triageService = new TriageService(engine, handler);
    VitalsEventConsumer consumer = new VitalsEventConsumer(triageService, objectMapper);

    VitalsReadingReceived event =
        new VitalsReadingReceived(
            "evt-1",
            "patient-1",
            "spo2",
            84.0,
            "%",
            Instant.parse("2026-06-16T12:00:00Z"),
            Instant.parse("2026-06-16T12:00:01Z"));
    FakeMessage message = new FakeMessage(objectMapper.writeValueAsString(event));

    consumer.handleMessage(message);

    ArgumentCaptor<VitalsReading> readingCaptor = ArgumentCaptor.forClass(VitalsReading.class);
    verify(engine).triage(readingCaptor.capture());
    assertThat(readingCaptor.getValue().patientId()).isEqualTo("patient-1");
    assertThat(readingCaptor.getValue().vitalType()).isEqualTo("spo2");
    assertThat(readingCaptor.getValue().value()).isEqualTo(84.0);

    verify(handler).handle(any(VitalsReading.class), any(TriageDecision.class));
    assertThat(message.acked).isTrue();
    assertThat(message.nacked).isFalse();
  }

  @Test
  void malformedPayloadIsNackedAndDoesNotInvokeEngine() {
    TriageEngine engine = Mockito.mock(TriageEngine.class);
    TriageResultHandler handler = Mockito.mock(TriageResultHandler.class);
    TriageService triageService = new TriageService(engine, handler);
    VitalsEventConsumer consumer = new VitalsEventConsumer(triageService, objectMapper);

    FakeMessage message = new FakeMessage("not-json");

    consumer.handleMessage(message);

    verify(engine, Mockito.never()).triage(any());
    verify(handler, Mockito.never()).handle(any(), any());
    assertThat(message.nacked).isTrue();
    assertThat(message.acked).isFalse();
  }
}
