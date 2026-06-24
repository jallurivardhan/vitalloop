package com.vitalloop.triage.adapter.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitalloop.triage.domain.RecommendedAction;
import com.vitalloop.triage.domain.Severity;
import com.vitalloop.triage.domain.TriageDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TriageDecisionParser}; no model calls. */
class TriageDecisionParserTest {

  private final TriageDecisionParser parser = new TriageDecisionParser();

  @Test
  @DisplayName("parses a clean JSON object")
  void parsesCleanJson() {
    String json =
        "{\"severity\":\"CRITICAL\",\"recommendedAction\":\"PHYSICIAN_ESCALATION\","
            + "\"rationale\":\"SpO2 84% is below the critical bound.\"}";

    TriageDecision decision = parser.parse(json);

    assertThat(decision.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.PHYSICIAN_ESCALATION);
    assertThat(decision.rationale()).contains("84%");
  }

  @Test
  @DisplayName("parses JSON wrapped in a ```json markdown fence")
  void parsesFencedJson() {
    String fenced =
        """
        ```json
        {"severity":"ELEVATED","recommendedAction":"PATIENT_NUDGE","rationale":"Slightly high."}
        ```
        """;

    TriageDecision decision = parser.parse(fenced);

    assertThat(decision.severity()).isEqualTo(Severity.ELEVATED);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.PATIENT_NUDGE);
  }

  @Test
  @DisplayName("parses JSON wrapped in a bare ``` fence and surrounding prose")
  void parsesBareFenceWithProse() {
    String messy =
        """
        Here is the result:
        ```
        {"severity":"NORMAL","recommendedAction":"NONE","rationale":"In range."}
        ```
        """;

    TriageDecision decision = parser.parse(messy);

    assertThat(decision.severity()).isEqualTo(Severity.NORMAL);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.NONE);
  }

  @Test
  @DisplayName("malformed output falls back to a safe UNKNOWN / NURSE_REVIEW decision")
  void malformedOutputFallsBack() {
    TriageDecision decision = parser.parse("I'm sorry, I can't help with that.");

    assertThat(decision.severity()).isEqualTo(Severity.UNKNOWN);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.NURSE_REVIEW);
    assertThat(decision.rationale()).isNotBlank();
  }

  @Test
  @DisplayName("unrecognized enum value falls back to a safe decision")
  void unknownEnumValueFallsBack() {
    String json =
        "{\"severity\":\"VERY_BAD\",\"recommendedAction\":\"CALL_911\",\"rationale\":\"x\"}";

    TriageDecision decision = parser.parse(json);

    assertThat(decision.severity()).isEqualTo(Severity.UNKNOWN);
    assertThat(decision.recommendedAction()).isEqualTo(RecommendedAction.NURSE_REVIEW);
  }

  @Test
  @DisplayName("empty / null output falls back to a safe decision")
  void emptyOutputFallsBack() {
    assertThat(parser.parse("").severity()).isEqualTo(Severity.UNKNOWN);
    assertThat(parser.parse(null).recommendedAction()).isEqualTo(RecommendedAction.NURSE_REVIEW);
  }
}
