package com.poc.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Incoming natural-language query from the UI. */
@Data
public class QueryRequest {

    @NotBlank(message = "Question must not be empty")
    private String question;
}
