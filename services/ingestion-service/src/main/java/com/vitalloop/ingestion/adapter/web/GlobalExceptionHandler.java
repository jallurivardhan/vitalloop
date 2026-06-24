package com.vitalloop.ingestion.adapter.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into a consistent {@link ErrorResponse} body.
 *
 * <p>Validation failures and malformed bodies map to {@code 400}; anything unexpected maps to a
 * safe {@code 500} that does not leak internal details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Bean Validation failures on the request body. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(
          fieldError.getField(),
          fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage());
    }
    return build(HttpStatus.BAD_REQUEST, "Validation failed for request body", fieldErrors);
  }

  /** Unparseable / malformed request body (e.g. invalid JSON). */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Malformed request body", Map.of());
  }

  /** Safe catch-all fallback. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of());
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, String message, Map<String, String> fieldErrors) {
    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            message,
            fieldErrors);
    return ResponseEntity.status(status).body(body);
  }
}
