package com.vitalloop.triage.adapter.triage;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.vitalloop.triage.domain.RecommendedAction;
import com.vitalloop.triage.domain.Severity;
import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import com.vitalloop.triage.port.TriageEngine;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link TriageEngine} backed by the Google ADK / Gemini on Vertex AI.
 *
 * <p>Builds a prompt from the reading, runs it through an {@link InMemoryRunner} over the
 * configured {@link LlmAgent}, accumulates the final streamed response, and parses it into a {@link
 * TriageDecision} via {@link TriageDecisionParser}. Any failure (including missing credentials)
 * yields a safe fallback rather than propagating.
 *
 * <p>Guarded by {@code vitalloop.triage.engine=adk}; the rule-based engine remains the default.
 */
@Component
@ConditionalOnProperty(name = "vitalloop.triage.engine", havingValue = "adk")
public class AdkTriageEngine implements TriageEngine {

  private static final String USER_ID = "ingestion-service";

  private final InMemoryRunner runner;
  private final TriageDecisionParser parser;

  public AdkTriageEngine(LlmAgent triageAgent) {
    this.runner = new InMemoryRunner(triageAgent);
    this.parser = new TriageDecisionParser();
  }

  @Override
  public TriageDecision triage(VitalsReading reading) {
    try {
      Session session =
          runner.sessionService().createSession(runner.appName(), USER_ID).blockingGet();
      Content message = Content.fromParts(Part.fromText(buildPrompt(reading)));

      StringBuilder finalResponse = new StringBuilder();
      runner
          .runAsync(session.userId(), session.id(), message)
          .blockingForEach(
              event -> {
                if (event.finalResponse()) {
                  finalResponse.append(event.stringifyContent());
                }
              });

      return parser.parse(finalResponse.toString());
    } catch (Exception e) {
      return new TriageDecision(
          Severity.UNKNOWN,
          RecommendedAction.NURSE_REVIEW,
          "ADK triage call failed (" + e.getMessage() + "); escalating for safe review.");
    }
  }

  /** Builds the user prompt describing the reading to triage. */
  static String buildPrompt(VitalsReading reading) {
    return String.format(
        Locale.ROOT,
        "Triage this vital-sign reading and respond with only the JSON object.%n"
            + "patientId: %s%nvitalType: %s%nvalue: %s%nunit: %s%nrecordedAt: %s",
        reading.patientId(),
        reading.vitalType(),
        reading.value(),
        reading.unit(),
        reading.recordedAt());
  }
}
