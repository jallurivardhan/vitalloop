package com.vitalloop.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the VitalLoop triage agent service. */
@SpringBootApplication
public class TriageAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(TriageAgentApplication.class, args);
  }
}
