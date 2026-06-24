package com.vitalloop.triage.port;

import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;

/**
 * Outbound port for handling a completed triage decision.
 *
 * <p>The only adapter today logs the decision. Future adapters will raise alerts, send
 * notifications, or write a FHIR DocumentReference — without changing the application layer.
 */
public interface TriageResultHandler {

  /**
   * Handles the triage decision for a reading.
   *
   * @param reading the reading that was triaged
   * @param decision the resulting decision
   */
  void handle(VitalsReading reading, TriageDecision decision);
}
