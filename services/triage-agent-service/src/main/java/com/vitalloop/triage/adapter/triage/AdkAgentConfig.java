package com.vitalloop.triage.adapter.triage;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * Builds the ADK {@link LlmAgent} used by {@link AdkTriageEngine}.
 *
 * <p>Guarded by {@code vitalloop.triage.engine=adk}: none of these beans load (and no Vertex
 * connection is attempted) when the default rule-based engine is active, so the build and CI stay
 * green without GCP credentials.
 */
@Configuration
@ConditionalOnProperty(name = "vitalloop.triage.engine", havingValue = "adk")
public class AdkAgentConfig {

  @Bean
  public LlmAgent triageAgent(
      @Value("${vitalloop.triage.model:gemini-2.5-flash}") String model,
      @Value("classpath:prompts/triage-agent-prompt.txt") Resource promptResource)
      throws IOException {
    String instruction =
        StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
    return LlmAgent.builder()
        .name("triage-agent")
        .description(
            "Clinician-support triage agent that classifies remote patient monitoring vital signs.")
        .model(model)
        .instruction(instruction)
        .tools(FunctionTool.create(CarePlanTools.class, "fetchThresholds"))
        .build();
  }
}
