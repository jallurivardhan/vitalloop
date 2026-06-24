package com.vitalloop.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vitalloop.contracts.VitalsReadingReceived;
import com.vitalloop.ingestion.domain.VitalsReading;
import com.vitalloop.ingestion.port.EventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit tests for the ingestion use case using a mocked {@link EventPublisher} (no Spring). */
class IngestionServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-16T12:00:00Z");

  @Test
  void defaultsRecordedAtAndPublishesEvent() {
    EventPublisher publisher = mock(EventPublisher.class);
    IngestionService service =
        new IngestionService(publisher, Clock.fixed(NOW, ZoneOffset.UTC), () -> "evt-123");

    VitalsReading reading = new VitalsReading("p-1", "heart_rate", 72.0, "bpm", null);

    IngestionAccepted accepted = service.accept(reading);

    assertThat(accepted.eventId()).isEqualTo("evt-123");

    ArgumentCaptor<VitalsReadingReceived> captor =
        ArgumentCaptor.forClass(VitalsReadingReceived.class);
    verify(publisher).publish(captor.capture());
    VitalsReadingReceived event = captor.getValue();

    assertThat(event.eventId()).isEqualTo("evt-123");
    assertThat(event.patientId()).isEqualTo("p-1");
    assertThat(event.vitalType()).isEqualTo("heart_rate");
    assertThat(event.value()).isEqualTo(72.0);
    assertThat(event.unit()).isEqualTo("bpm");
    // recordedAt was absent -> defaulted to now; occurredAt is always now.
    assertThat(event.recordedAt()).isEqualTo(NOW);
    assertThat(event.occurredAt()).isEqualTo(NOW);
  }

  @Test
  void preservesProvidedRecordedAt() {
    EventPublisher publisher = mock(EventPublisher.class);
    Instant provided = Instant.parse("2026-05-01T08:30:00Z");
    IngestionService service =
        new IngestionService(publisher, Clock.fixed(NOW, ZoneOffset.UTC), () -> "evt-1");

    service.accept(new VitalsReading("p-1", "spo2", 97.0, "%", provided));

    ArgumentCaptor<VitalsReadingReceived> captor =
        ArgumentCaptor.forClass(VitalsReadingReceived.class);
    verify(publisher).publish(captor.capture());

    assertThat(captor.getValue().recordedAt()).isEqualTo(provided);
    assertThat(captor.getValue().occurredAt()).isEqualTo(NOW);
  }
}
