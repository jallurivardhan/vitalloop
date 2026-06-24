package com.vitalloop.triage.adapter.triage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitalloop.triage.domain.RecommendedAction;
import com.vitalloop.triage.domain.Severity;
import com.vitalloop.triage.domain.TriageDecision;
import java.util.Locale;

/**
 * Parses the triage agent's textual output into a {@link TriageDecision}.
 *
 * <p>Deliberately separated from {@link AdkTriageEngine} so it can be unit-tested without invoking
 * the model. Tolerates markdown code fences around the JSON. Any output that cannot be parsed into
 * a valid decision yields a safe fallback ({@link Severity#UNKNOWN} / {@link
 * RecommendedAction#NURSE_REVIEW}).
 */
class TriageDecisionParser {

  private final ObjectMapper objectMapper = new ObjectMapper();

  TriageDecision parse(String modelOutput) {
    if (modelOutput == null || modelOutput.isBlank()) {
      return fallback("Triage model returned no output; escalating for safe review.");
    }

    String json = extractJson(stripMarkdownFences(modelOutput));
    try {
      JsonNode node = objectMapper.readTree(json);
      Severity severity = parseEnum(Severity.class, node.path("severity").asText(null));
      RecommendedAction action =
          parseEnum(RecommendedAction.class, node.path("recommendedAction").asText(null));
      if (severity == null || action == null) {
        return fallback("Triage model output missing severity/recommendedAction; escalating.");
      }
      String rationale = node.path("rationale").asText("");
      return new TriageDecision(severity, action, rationale);
    } catch (Exception e) {
      return fallback("Could not parse triage model output as JSON; escalating for safe review.");
    }
  }

  /** Removes a leading/trailing markdown code fence (``` or ```json) if present. */
  static String stripMarkdownFences(String text) {
    String trimmed = text.strip();
    if (!trimmed.startsWith("```")) {
      return trimmed;
    }
    int firstNewline = trimmed.indexOf('\n');
    String withoutOpening =
        firstNewline >= 0
            ? trimmed.substring(firstNewline + 1)
            : trimmed.replaceFirst("^```\\w*", "");
    int closingFence = withoutOpening.lastIndexOf("```");
    String withoutClosing =
        closingFence >= 0 ? withoutOpening.substring(0, closingFence) : withoutOpening;
    return withoutClosing.strip();
  }

  /** Best-effort: narrows to the outermost JSON object if extra prose surrounds it. */
  private static String extractJson(String text) {
    int start = text.indexOf('{');
    int end = text.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return text.substring(start, end + 1);
    }
    return text;
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(type, raw.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private TriageDecision fallback(String rationale) {
    return new TriageDecision(Severity.UNKNOWN, RecommendedAction.NURSE_REVIEW, rationale);
  }
}
