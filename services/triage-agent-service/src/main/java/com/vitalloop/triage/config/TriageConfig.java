package com.vitalloop.triage.config;

import com.vitalloop.triage.application.TriageService;
import com.vitalloop.triage.port.TriageEngine;
import com.vitalloop.triage.port.TriageResultHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free application layer into the Spring context, preserving the hexagonal
 * boundary (the application and domain layers stay free of Spring).
 */
@Configuration
public class TriageConfig {

  @Bean
  public TriageService triageService(TriageEngine triageEngine, TriageResultHandler resultHandler) {
    return new TriageService(triageEngine, resultHandler);
  }
}
