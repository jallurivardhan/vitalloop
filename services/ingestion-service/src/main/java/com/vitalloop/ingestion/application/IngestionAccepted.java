package com.vitalloop.ingestion.application;

/**
 * Result of accepting a reading for asynchronous processing.
 *
 * @param eventId identifier of the published {@code vitals.reading.received} event; clients can use
 *     it to correlate the reading with downstream triage results
 */
public record IngestionAccepted(String eventId) {}
