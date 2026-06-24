package com.vitalloop.triage.domain;

/**
 * Threshold bands for a single vital type used to classify a reading.
 *
 * <p>Values strictly within {@code [normalLow, normalHigh]} are {@link Severity#NORMAL}. Values
 * strictly below {@code criticalLow} or strictly above {@code criticalHigh} are {@link
 * Severity#CRITICAL}. Values in between are graded {@link Severity#ELEVATED} or {@link
 * Severity#URGENT} by how close they sit to the critical boundary.
 *
 * @param normalLow inclusive lower bound of the normal range
 * @param normalHigh inclusive upper bound of the normal range
 * @param criticalLow value below which the reading is critical
 * @param criticalHigh value above which the reading is critical
 * @param unit the unit these thresholds are expressed in
 */
public record CarePlanThresholds(
    double normalLow, double normalHigh, double criticalLow, double criticalHigh, String unit) {}
