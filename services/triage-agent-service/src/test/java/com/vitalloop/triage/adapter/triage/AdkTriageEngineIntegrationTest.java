package com.vitalloop.triage.adapter.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitalloop.triage.domain.TriageDecision;
import com.vitalloop.triage.domain.VitalsReading;
import com.vitalloop.triage.port.TriageEngine;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Live integration test that actually calls Vertex AI through the ADK engine.
 *
 * <p>Gated two ways so it never runs in CI or without credentials:
 *
 * <ul>
 *   <li>{@code @Tag("integration")} â€” excluded by Surefire's {@code excludedGroups} by default.
 *   <li>{@code @EnabledIfEnvironmentVariable} â€” skipped unless {@code GOOGLE_CLOUD_PROJECT} is
 *       set.
 * </ul>
 *
 * <p>Running it requires {@code GOOGLE_GENAI_USE_VERTEXAI=true}, {@code GOOGLE_CLOUD_PROJECT},
 * {@code GOOGLE_CLOUD_LOCATION}, and Application Default Credentials. It incurs Vertex AI cost.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_CLOUD_PROJECT", matches = ".+")
@SpringBootTest(properties = "vitalloop.triage.engine=adk")
class AdkTriageEngineIntegrationTest {

  @Autowired private TriageEngine triageEngine;

  @Test
  @DisplayName("ADK engine is wired and returns a decision for a critical reading")
  void triagesCriticalReadingViaVertexAi() {
    assertThat(triageEngine).isInstanceOf(AdkTriageEngine.class);

    VitalsReading reading =
        new VitalsReading("patient-1", "spo2", 84.0, "%", Instant.parse("2026-06-16T12:00:00Z"));

    TriageDecision decision = triageEngine.triage(reading);

    assertThat(decision).isNotNull();
    assertThat(decision.severity()).isNotNull();
    assertThat(decision.recommendedAction()).isNotNull();
  }
}
