package com.vitalloop.triage.adapter.result;

import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import com.vitalloop.triage.port.TriageResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link TriageResultHandler} that logs the decision as structured JSON.
 *
 * <p>Uses the SLF4J fluent API; the key-value pairs are emitted as fields by Spring Boot's ECS
 * structured logging. A placeholder until alerting / notifications / FHIR documentation adapters
 * land in later phases.
 */
@Component
public class LoggingTriageResultHandler implements TriageResultHandler {

  private static final Logger log = LoggerFactory.getLogger(LoggingTriageResultHandler.class);

  @Override
  public void handle(VitalsReading reading, TriageDecision decision) {
    log.atInfo()
        .setMessage("triage decision produced")
        .addKeyValue("patientId", reading.patientId())
        .addKeyValue("vitalType", reading.vitalType())
        .addKeyValue("value", reading.value())
        .addKeyValue("unit", reading.unit())
        .addKeyValue("severity", decision.severity())
        .addKeyValue("recommendedAction", decision.recommendedAction())
        .addKeyValue("rationale", decision.rationale())
        .log();
  }
}
