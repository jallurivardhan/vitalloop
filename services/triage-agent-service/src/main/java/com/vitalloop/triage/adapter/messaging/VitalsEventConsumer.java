package com.vitalloop.triage.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.vitalloop.contracts.VitalsReadingReceived;
import com.vitalloop.triage.application.TriageService;
import com.vitalloop.triage.domain.VitalsReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Inbound messaging adapter: turns a received {@code vitals.reading.received} Pub/Sub message into
 * a domain {@link VitalsReading} and drives the triage use case.
 *
 * <p>The message-handling logic lives here (and is unit-testable); the actual subscription wiring
 * is done by {@link PubSubSubscriberInitializer}, which only runs under the {@code local} profile.
 */
@Component
public class VitalsEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(VitalsEventConsumer.class);

  private final TriageService triageService;
  private final ObjectMapper objectMapper;

  public VitalsEventConsumer(TriageService triageService, ObjectMapper objectMapper) {
    this.triageService = triageService;
    this.objectMapper = objectMapper;
  }

  /**
   * Handles a single Pub/Sub message: deserializes the event, triages it, and acknowledges. On a
   * deserialization failure the message is nacked so it can be redelivered / dead-lettered.
   */
  public void handleMessage(BasicAcknowledgeablePubsubMessage message) {
    String payload = message.getPubsubMessage().getData().toStringUtf8();
    try {
      process(payload);
      message.ack();
    } catch (Exception e) {
      log.atError()
          .setMessage("failed to process vitals.reading.received event; nacking")
          .addKeyValue("error", e.getMessage())
          .log();
      message.nack();
    }
  }

  /**
   * Deserializes the event JSON, reconstructs a reading, and runs triage. Package-private and
   * message-infrastructure-free so it can be unit-tested directly.
   */
  void process(String json) throws com.fasterxml.jackson.core.JsonProcessingException {
    VitalsReadingReceived event = objectMapper.readValue(json, VitalsReadingReceived.class);
    // Carry the eventId in the logging context so every downstream log line
    // (including the triage decision) can be correlated back to this event.
    MDC.put("eventId", event.eventId());
    try {
      // eventId is supplied by MDC (above); adding it again here would be a
      // duplicate structured key.
      log.atInfo()
          .setMessage("received vitals.reading.received event")
          .addKeyValue("patientId", event.patientId())
          .addKeyValue("vitalType", event.vitalType())
          .addKeyValue("value", event.value())
          .log();
      VitalsReading reading =
          new VitalsReading(
              event.patientId(),
              event.vitalType(),
              event.value(),
              event.unit(),
              event.recordedAt());
      triageService.process(reading);
    } finally {
      MDC.remove("eventId");
    }
  }
}
