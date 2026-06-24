package com.vitalloop.triage.adapter.triage;

import com.vitalloop.triage.domain.CarePlanThresholds;
import com.vitalloop.triage.domain.DefaultCarePlanThresholds;
import com.vitalloop.triage.domain.RecommendedAction;
import com.vitalloop.triage.domain.Severity;
import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import com.vitalloop.triage.port.TriageEngine;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic, rule-based {@link TriageEngine} stub.
 *
 * <p>This is a placeholder for the future ADK/Vertex AI engine. It classifies a reading against
 * hardcoded default thresholds per vital type and maps the resulting {@link Severity} to the least
 * aggressive <em>safe</em> {@link RecommendedAction}. When the reading cannot be classified
 * (missing value or an unknown vital type) the decision is treated as uncertain and the action is
 * escalated one level for safety.
 *
 * <p>Activated by default (and whenever {@code vitalloop.triage.engine=rule-based}); a future
 * engine can take over by setting that property to a different value.
 */
@Component
@ConditionalOnProperty(
    name = "vitalloop.triage.engine",
    havingValue = "rule-based",
    matchIfMissing = true)
public class RuleBasedTriageEngine implements TriageEngine {

  @Override
  public TriageDecision triage(VitalsReading reading) {
    if (reading == null || reading.vitalType() == null || reading.value() == null) {
      return uncertain("Insufficient data to triage; escalating one level for safe review.");
    }

    String key = DefaultCarePlanThresholds.normalize(reading.vitalType());
    Optional<CarePlanThresholds> maybeThresholds =
        DefaultCarePlanThresholds.forVitalType(reading.vitalType());
    if (maybeThresholds.isEmpty()) {
      return uncertain(
          "Unknown vital type '"
              + reading.vitalType()
              + "'; no thresholds configured, escalating one level for safe review.");
    }
    CarePlanThresholds thresholds = maybeThresholds.get();

    double value = reading.value();
    Severity severity = classify(value, thresholds);
    RecommendedAction action = baselineAction(severity);
    String rationale =
        String.format(
            Locale.ROOT,
            "%s %.1f %s classified %s against normal [%.1f, %.1f] %s; critical outside [%.1f,"
                + " %.1f].",
            key,
            value,
            reading.unit(),
            severity,
            thresholds.normalLow(),
            thresholds.normalHigh(),
            thresholds.unit(),
            thresholds.criticalLow(),
            thresholds.criticalHigh());
    return new TriageDecision(severity, action, rationale);
  }

  /** Classifies a value into a severity band using the supplied thresholds. */
  private Severity classify(double value, CarePlanThresholds t) {
    if (value < t.criticalLow() || value > t.criticalHigh()) {
      return Severity.CRITICAL;
    }
    if (value < t.normalLow()) {
      double midpoint = (t.criticalLow() + t.normalLow()) / 2.0;
      return value < midpoint ? Severity.URGENT : Severity.ELEVATED;
    }
    if (value > t.normalHigh()) {
      double midpoint = (t.normalHigh() + t.criticalHigh()) / 2.0;
      return value > midpoint ? Severity.URGENT : Severity.ELEVATED;
    }
    return Severity.NORMAL;
  }

  /** Maps a severity to the least aggressive safe action. */
  private RecommendedAction baselineAction(Severity severity) {
    return switch (severity) {
      case NORMAL -> RecommendedAction.NONE;
      case ELEVATED -> RecommendedAction.PATIENT_NUDGE;
      case URGENT -> RecommendedAction.NURSE_REVIEW;
      case CRITICAL -> RecommendedAction.PHYSICIAN_ESCALATION;
      // Uncertain readings start from a "patient nudge" baseline and are escalated below.
      case UNKNOWN -> RecommendedAction.PATIENT_NUDGE;
    };
  }

  /** Builds an uncertain decision: severity UNKNOWN with the action escalated one level. */
  private TriageDecision uncertain(String rationale) {
    RecommendedAction escalated = escalate(baselineAction(Severity.UNKNOWN));
    return new TriageDecision(Severity.UNKNOWN, escalated, rationale);
  }

  /**
   * Returns the next more aggressive action, capped at {@link
   * RecommendedAction#PHYSICIAN_ESCALATION}.
   */
  private RecommendedAction escalate(RecommendedAction action) {
    RecommendedAction[] ladder = RecommendedAction.values();
    int next = Math.min(action.ordinal() + 1, ladder.length - 1);
    return ladder[next];
  }
}
