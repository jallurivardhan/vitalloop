package com.vitalloop.triage.domain;

/**
 * Clinical severity classification assigned to a vital-sign reading.
 *
 * <p>Ordered from least to most concerning. {@link #UNKNOWN} is used when the reading cannot be
 * classified (e.g. an unrecognized vital type) and signals that the triage decision is uncertain.
 */
public enum Severity {
  NORMAL,
  ELEVATED,
  URGENT,
  CRITICAL,
  UNKNOWN
}
