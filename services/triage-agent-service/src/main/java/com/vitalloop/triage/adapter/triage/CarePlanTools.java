package com.vitalloop.triage.adapter.triage;

import com.google.adk.tools.Annotations.Schema;
import com.vitalloop.triage.domain.CarePlanThresholds;
import com.vitalloop.triage.domain.DefaultCarePlanThresholds;

/**
 * ADK function-calling tools exposed to the triage agent.
 *
 * <p>The {@link Schema} annotations describe the tool and its parameters so the model knows when
 * and how to call it. Currently backed by the built-in {@link DefaultCarePlanThresholds}; in a
 * later phase this will read patient-specific thresholds from the FHIR store.
 */
public final class CarePlanTools {

  private CarePlanTools() {}

  /**
   * Returns the care-plan threshold bands for a patient's vital type. The agent should call this
   * before classifying a reading. When no thresholds are configured for the vital type, the
   * returned object has {@code unit = "unknown"} and zeroed bounds, signalling that the reading is
   * uncertain and should be escalated.
   */
  @Schema(
      description =
          "Fetch the care-plan threshold bands (normal and critical ranges) for a given patient"
              + " and vital type. Call this first, before classifying a reading. If the returned"
              + " unit is 'unknown', no thresholds are configured and the reading is uncertain.")
  public static CarePlanThresholds fetchThresholds(
      @Schema(name = "patientId", description = "Opaque identifier of the patient.")
          String patientId,
      @Schema(
              name = "vitalType",
              description =
                  "The vital type, e.g. blood_pressure_systolic, blood_glucose, heart_rate, spo2.")
          String vitalType) {
    return DefaultCarePlanThresholds.forVitalTypeOrUnknown(vitalType);
  }
}
