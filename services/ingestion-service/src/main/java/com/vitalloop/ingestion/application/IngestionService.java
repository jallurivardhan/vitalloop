package com.vitalloop.ingestion.application;

import com.vitalloop.contracts.VitalsReadingReceived;
import com.vitalloop.ingestion.domain.VitalsReading;
import com.vitalloop.ingestion.port.EventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ingestion use case: enriches an incoming reading (defaulting {@code recordedAt} to now when
 * absent, generating an event id) and publishes a {@link VitalsReadingReceived} event. Triage now
 * happens asynchronously in the triage-agent-service, so this no longer triages on the request
 * path.
 *
 * <p>Framework-free; wired as a Spring bean in the configuration layer.
 */
public class IngestionService {

  private final EventPublisher eventPublisher;
  private final Clock clock;
  private final Supplier<String> eventIdGenerator;

  public IngestionService(EventPublisher eventPublisher) {
    this(eventPublisher, Clock.systemUTC(), () -> UUID.randomUUID().toString());
  }

  /** Visible for testing: fixed clock and deterministic ids make the output verifiable. */
  IngestionService(EventPublisher eventPublisher, Clock clock, Supplier<String> eventIdGenerator) {
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.eventIdGenerator = Objects.requireNonNull(eventIdGenerator, "eventIdGenerator");
  }

  /**
   * Accepts a reading: applies defaults, publishes the event, and returns the assigned event id.
   *
   * @param reading the incoming reading (must not be {@code null})
   * @return the acceptance result carrying the published event id
   */
  public IngestionAccepted accept(VitalsReading reading) {
    Objects.requireNonNull(reading, "reading");
    Instant now = Instant.now(clock);
    Instant recordedAt = reading.recordedAt() == null ? now : reading.recordedAt();
    String eventId = eventIdGenerator.get();

    VitalsReadingReceived event =
        new VitalsReadingReceived(
            eventId,
            reading.patientId(),
            reading.vitalType(),
            reading.value(),
            reading.unit(),
            recordedAt,
            now);

    eventPublisher.publish(event);
    return new IngestionAccepted(eventId);
  }
}
