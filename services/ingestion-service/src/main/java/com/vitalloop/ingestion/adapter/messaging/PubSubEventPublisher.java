package com.vitalloop.ingestion.adapter.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.vitalloop.contracts.VitalsReadingReceived;
import com.vitalloop.ingestion.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Outbound messaging adapter that publishes events to Google Cloud Pub/Sub as JSON.
 *
 * <p>Publishing is asynchronous (the request returns 202 before delivery is confirmed); delivery
 * failures are logged so they are observable without blocking the request thread.
 */
@Component
public class PubSubEventPublisher implements EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(PubSubEventPublisher.class);

  private final PubSubTemplate pubSubTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public PubSubEventPublisher(
      PubSubTemplate pubSubTemplate,
      ObjectMapper objectMapper,
      @Value("${vitalloop.pubsub.topic:vitals-reading-received}") String topic) {
    this.pubSubTemplate = pubSubTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  @Override
  public void publish(VitalsReadingReceived event) {
    String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new EventSerializationException(event.eventId(), e);
    }
    pubSubTemplate
        .publish(topic, payload)
        .whenComplete(
            (id, error) -> {
              if (error != null) {
                log.atError()
                    .setMessage("failed to publish vitals.reading.received event")
                    .addKeyValue("eventId", event.eventId())
                    .addKeyValue("topic", topic)
                    .addKeyValue("error", error.getMessage())
                    .log();
              }
            });
  }

  /** Thrown when an event cannot be serialized to JSON. */
  static class EventSerializationException extends RuntimeException {
    EventSerializationException(String eventId, Throwable cause) {
      super("Failed to serialize event " + eventId, cause);
    }
  }
}
