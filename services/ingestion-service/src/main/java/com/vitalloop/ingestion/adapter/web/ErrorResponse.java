package com.vitalloop.ingestion.adapter.web;

import java.util.Map;

/**
 * Structured error body returned by {@link GlobalExceptionHandler}.
 *
 * @param timestamp ISO-8601 instant the error was produced
 * @param status HTTP status code
 * @param error HTTP reason phrase
 * @param message human-readable summary
 * @param fieldErrors per-field validation messages (empty when not applicable)
 */
public record ErrorResponse(
    String timestamp, int status, String error, String message, Map<String, String> fieldErrors) {}
