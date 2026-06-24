package com.vitalloop.ingestion.adapter.web;

import com.vitalloop.ingestion.application.IngestionAccepted;
import com.vitalloop.ingestion.application.IngestionService;
import com.vitalloop.ingestion.domain.VitalsReading;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inbound REST adapter exposing the vital-sign ingestion endpoint. */
@RestController
@RequestMapping("/api/v1")
public class IngestionController {

  private final IngestionService ingestionService;

  public IngestionController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  /**
   * Accepts a single vital-sign reading for asynchronous triage.
   *
   * @param reading the validated request body
   * @return {@code 202 Accepted} with the published event id
   */
  @PostMapping("/vitals")
  public ResponseEntity<IngestionAccepted> ingestVitals(@Valid @RequestBody VitalsReading reading) {
    IngestionAccepted accepted = ingestionService.accept(reading);
    return ResponseEntity.accepted().body(accepted);
  }
}
