package com.vitalloop.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Smoke test: the full application context loads with the default (rule-based) triage engine. */
@SpringBootTest
class IngestionApplicationTests {

  @Test
  void contextLoads() {
    // Intentionally empty: fails if the Spring context cannot be created.
  }
}
