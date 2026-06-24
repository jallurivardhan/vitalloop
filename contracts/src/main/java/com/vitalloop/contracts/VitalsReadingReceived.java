package com.vitalloop.contracts;

import java.time.Instant;

/**
 * Event published when the ingestion service accepts a vital-sign reading.
 *
 * <p>This is the wire contract shared by the publisher (ingestion-service) and consumers
 * (triage-agent-service). It is intentionally framework-free (plain Java + Jackson) and carries no
 * behaviour — only the data needed to triage a reading downstream.
 *
 * <p>Topic: {@code vitals-reading-received}.
 *
 * @param eventId unique identifier for this event (idempotency key)
 * @param patientId identifier of the patient the reading belongs to
 * @param vitalType the kind of measurement, e.g. {@code blood_pressure_systolic}, {@code spo2}
 * @param value the measured numeric value
 * @param unit the unit of measure, e.g. {@code mmHg}, {@code bpm}, {@code %}
 * @param recordedAt when the reading was taken
 * @param occurredAt when the event was produced (i.e. when ingestion accepted it)
 */
public record VitalsReadingReceived(
    String eventId,
    String patientId,
    String vitalType,
    Double value,
    String unit,
    Instant recordedAt,
    Instant occurredAt) {}
