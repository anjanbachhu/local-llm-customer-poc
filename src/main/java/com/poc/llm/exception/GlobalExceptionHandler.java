package com.poc.llm.exception;

import com.poc.llm.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

/**
 * Translates exceptions thrown anywhere in the controller layer into a uniform
 * JSON {@link ErrorResponse} with an appropriate HTTP status.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JsonParsingException.class)
    public ResponseEntity<ErrorResponse> handleJsonParsing(JsonParsingException ex) {
        log.warn("JSON parsing failed: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid JSON", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "Validation error", message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Upload too large",
                "One or more files exceed the configured upload size limit.");
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ErrorResponse> handleLlm(LlmException ex) {
        log.error("LLM failure", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "LLM error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred. Check the server logs for details.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
