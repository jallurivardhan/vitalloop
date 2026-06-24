package com.vitalloop.triage.domain;

/**
 * The outcome of triaging a {@link VitalsReading}.
 *
 * @param severity the assigned clinical severity
 * @param recommendedAction the follow-up action to take
 * @param rationale a short, human-readable explanation of how the decision was reached
 */
public record TriageDecision(
    Severity severity, RecommendedAction recommendedAction, String rationale) {}
