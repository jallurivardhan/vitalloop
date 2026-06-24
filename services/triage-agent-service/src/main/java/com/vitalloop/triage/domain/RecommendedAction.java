package com.vitalloop.triage.domain;

/**
 * Recommended follow-up action for a triaged reading.
 *
 * <p>Ordered from least to most aggressive. The triage engine maps a {@link Severity} to the least
 * aggressive <em>safe</em> action, escalating one level when the decision is uncertain.
 */
public enum RecommendedAction {
  NONE,
  PATIENT_NUDGE,
  NURSE_REVIEW,
  PHYSICIAN_ESCALATION
}
