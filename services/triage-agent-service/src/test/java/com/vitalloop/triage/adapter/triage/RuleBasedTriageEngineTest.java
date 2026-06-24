package com.vitalloop.triage.adapter.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitalloop.triage.domain.RecommendedAction;
import com.vitalloop.triage.domain.Severity;
import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RuleBasedTriageEngineTest {

  private final RuleBasedTriageEngine engine = new RuleBasedTriageEngine();

  private static VitalsReading reading(String vitalType, Double value, String unit) {
    return new VitalsReading(
        "patient-1", vitalType, value, unit, Instant.parse("2026-01-01T00:00:00Z"));
  }

  @DisplayName("blood_pressure_systolic: classifies each severity band and boundary correctly")
  @ParameterizedTest(name = "value={0} -> {1}/{2}")
  @CsvSource({
    // thresholds: normal [90,120], critical outside [70,180]; midLow=80, midHigh=150
    "110, NORMAL,   NONE", // mid-normal
    "90,  NORMAL,   NONE", // lower normal boundary (inclusive)
    "120, NORMAL,   NONE", // upper normal boundary (inclusive)
    "130, ELEVATED, PATIENT_NUDGE", // mild high
    "150, ELEVATED, PATIENT_NUDGE", // exactly midpoint high -> still elevated
    "170, URGENT,   NURSE_REVIEW", // strongly high
    "180, URGENT,   NURSE_REVIEW", // exactly criticalHigh boundary -> not yet critical
    "181, CRITICAL, PHYSICIAN_ESCALATION", // above criticalHigh
    "85,  ELEVATED, PATIENT_NUDGE", // mild low
    "80,  ELEVATED, PATIENT_NUDGE", // exactly midpoint low -> still elevated
    "75,  URGENT,   NURSE_REVIEW", // strongly low
    "70,  URGENT,   NURSE_REVIEW", // exactly criticalLow boundary -> not yet critical
    "69,  CRITICAL, PHYSICIAN_ESCALATION", // below criticalLow
  })
  void classifiesBloodPressureBands(
      double value, Severity expectedSeverity, RecommendedAction expectedAction) {
    TriageDecision decision = engine.triage(reading("blood_pressure_systolic", value, "mmHg"));

    assertThat(decision.severity()).isEqualTo(expectedSeverity);
    assertThat(decision.recommendedAction()).isEqualTo(expectedAction);
    assertThat(decision.rationale()).isNotBlank();
  }

  @DisplayName("spo2: low readings escalate through the bands")
  @ParameterizedTest(name = "value={0} -> {1}")
  @CsvSource({
    // thresholds: normal [95,100], critical below 88; midLow=91.5
    "98, NORMAL",
    "92, ELEVATED",
    "90, URGENT",
    "87, CRITICAL",
  })
  void classifiesSpo2(double value, Severity expectedSeverity) {
    TriageDecision decision = engine.triage(reading("spo2", value, "%"));
    assertThat(decision.severity()).isEqualTo(expectedSeverity);
  }

  @Test
  @DisplayName("case-insensitive vital type is still recognized")
  void normalizesVitalTypeCase() {
    TriageDecision decision = engine.triage(reading("Heart_Rate", 72.0, "bpm"));
    assertThat(decision.severity()).isEqualTo(Severity.NORMAL);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.NONE);
  }

  @Test
  @DisplayName("unknown vital type -> UNKNOWN severity escalated one level to NURSE_REVIEW")
  void escalatesOnUnknownVitalType() {
    TriageDecision decision = engine.triage(reading("body_temperature", 37.0, "C"));

    assertThat(decision.severity()).isEqualTo(Severity.UNKNOWN);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.NURSE_REVIEW);
    assertThat(decision.rationale()).contains("Unknown vital type");
  }

  @Test
  @DisplayName("missing value -> UNKNOWN severity escalated one level to NURSE_REVIEW")
  void escalatesOnMissingValue() {
    TriageDecision decision = engine.triage(reading("heart_rate", null, "bpm"));

    assertThat(decision.severity()).isEqualTo(Severity.UNKNOWN);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.NURSE_REVIEW);
  }
}
