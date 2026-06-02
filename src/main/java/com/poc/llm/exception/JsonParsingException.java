package com.poc.llm.exception;

/** Thrown when an uploaded file cannot be parsed as customer JSON. */
public class JsonParsingException extends RuntimeException {

    public JsonParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonParsingException(String message) {
        super(message);
    }
}
