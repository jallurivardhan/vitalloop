package com.vitalloop.triage.application;

import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import com.vitalloop.triage.port.TriageEngine;
import com.vitalloop.triage.port.TriageResultHandler;
import java.util.Objects;

/**
 * Triage use case: triages a reading via the {@link TriageEngine} port and forwards the decision to
 * the {@link TriageResultHandler} port.
 *
 * <p>Framework-free; wired as a Spring bean in the configuration layer.
 */
public class TriageService {

  private final TriageEngine triageEngine;
  private final TriageResultHandler resultHandler;

  public TriageService(TriageEngine triageEngine, TriageResultHandler resultHandler) {
    this.triageEngine = Objects.requireNonNull(triageEngine, "triageEngine");
    this.resultHandler = Objects.requireNonNull(resultHandler, "resultHandler");
  }

  /**
   * Triages a reading and hands the decision to the result handler.
   *
   * @param reading the reading to triage (must not be {@code null})
   * @return the triage decision
   */
  public TriageDecision process(VitalsReading reading) {
    Objects.requireNonNull(reading, "reading");
    TriageDecision decision = triageEngine.triage(reading);
    resultHandler.handle(reading, decision);
    return decision;
  }
}
