package com.vitalloop.triage.domain;

import java.time.Instant;

/**
 * A vital-sign reading to be triaged, reconstructed by the inbound messaging adapter from a
 * received event.
 *
 * <p>Unlike the ingestion service's inbound model, this domain type carries no HTTP/validation
 * concerns — it is the internal representation the triage engine operates on.
 *
 * @param patientId identifier of the patient the reading belongs to
 * @param vitalType the kind of measurement, e.g. {@code blood_pressure_systolic}, {@code spo2}
 * @param value the measured numeric value (may be {@code null} if absent in the event)
 * @param unit the unit of measure, e.g. {@code mmHg}, {@code bpm}, {@code %}
 * @param recordedAt when the reading was taken
 */
public record VitalsReading(
    String patientId, String vitalType, Double value, String unit, Instant recordedAt) {}
