package com.vitalloop.ingestion.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * A single vital-sign reading submitted for ingestion.
 *
 * <p>Bean Validation constraints are declared on the record components so this type can be used
 * directly as a validated inbound request body. {@code recordedAt} is optional on input and is
 * defaulted by the application layer when absent.
 *
 * @param patientId opaque identifier of the patient the reading belongs to
 * @param vitalType the kind of measurement, e.g. {@code blood_pressure_systolic}, {@code spo2}
 * @param value the measured numeric value
 * @param unit the unit of measure, e.g. {@code mmHg}, {@code bpm}, {@code %}
 * @param recordedAt when the reading was taken; defaulted to "now" when not supplied
 */
public record VitalsReading(
    @NotBlank String patientId,
    @NotBlank String vitalType,
    @NotNull @Positive Double value,
    @NotBlank String unit,
    Instant recordedAt) {

  /** Returns a copy of this reading with {@code recordedAt} replaced. */
  public VitalsReading withRecordedAt(Instant newRecordedAt) {
    return new VitalsReading(patientId, vitalType, value, unit, newRecordedAt);
  }
}
