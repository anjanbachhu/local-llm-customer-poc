package com.poc.llm.exception;

/** Thrown when the local LLM fails in a way that cannot be recovered from. */
public class LlmException extends RuntimeException {

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmException(String message) {
        super(message);
    }
}
