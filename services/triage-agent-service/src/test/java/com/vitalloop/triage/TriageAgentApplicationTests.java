package com.vitalloop.triage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the full application context loads with the default (rule-based) engine and the
 * Pub/Sub emulator host configured, without requiring GCP credentials or a running emulator (the
 * live subscriber and topic bootstrap are {@code local}-profile only).
 */
@SpringBootTest
class TriageAgentApplicationTests {

  @Test
  void contextLoads() {
    // Fails if the Spring context cannot be created.
  }
}
