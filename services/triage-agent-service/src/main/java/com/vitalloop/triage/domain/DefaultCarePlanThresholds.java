package com.vitalloop.triage.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Single source of truth for the built-in, per-vital-type default {@link CarePlanThresholds}.
 *
 * <p>Framework-free and shared by every triage engine so the rule-based and ADK adapters classify
 * against identical bands. In a later phase these defaults will be superseded by patient-specific
 * thresholds read from the FHIR store.
 */
public final class DefaultCarePlanThresholds {

  /** Sentinel returned when no thresholds are configured for a vital type. */
  public static final CarePlanThresholds UNKNOWN = new CarePlanThresholds(0, 0, 0, 0, "unknown");

  private static final Map<String, CarePlanThresholds> THRESHOLDS =
      Map.of(
          "blood_pressure_systolic", new CarePlanThresholds(90, 120, 70, 180, "mmHg"),
          "blood_glucose", new CarePlanThresholds(70, 140, 54, 250, "mg/dL"),
          "heart_rate", new CarePlanThresholds(60, 100, 40, 130, "bpm"),
          "spo2", new CarePlanThresholds(95, 100, 88, 100, "%"));

  private DefaultCarePlanThresholds() {}

  /** Normalizes a vital type to its canonical lookup key. */
  public static String normalize(String vitalType) {
    return vitalType.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the default thresholds for the given vital type, if any are configured.
   *
   * @param vitalType the vital type (case-insensitive); may be {@code null}
   * @return the thresholds, or empty when the vital type is unknown
   */
  public static Optional<CarePlanThresholds> forVitalType(String vitalType) {
    if (vitalType == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(THRESHOLDS.get(normalize(vitalType)));
  }

  /**
   * Like {@link #forVitalType(String)} but returns the {@link #UNKNOWN} sentinel instead of empty,
   * for callers (such as function-calling tools) that must always return a value.
   */
  public static CarePlanThresholds forVitalTypeOrUnknown(String vitalType) {
    return forVitalType(vitalType).orElse(UNKNOWN);
  }
}
