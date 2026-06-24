package com.vitalloop.triage.port;

import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;

/**
 * Outbound port for triaging a vital-sign reading.
 *
 * <p>The application depends only on this interface. The current implementation is a deterministic,
 * rule-based stub; a future ADK/Vertex AI-backed adapter can replace it without changing the
 * application or domain layers.
 */
public interface TriageEngine {

  /**
   * Triages a reading and returns a decision. Implementations must be side-effect free and must
   * never throw for ordinary inputs; uncertain inputs should be reflected in the returned {@link
   * TriageDecision} rather than as exceptions.
   *
   * @param reading the reading to triage (never {@code null})
   * @return the triage decision
   */
  TriageDecision triage(VitalsReading reading);
}
