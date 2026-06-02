package com.poc.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** Uniform error payload returned by the global exception handler. */
@Data
@Builder
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
}
