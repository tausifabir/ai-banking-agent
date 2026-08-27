package com.aibankingagent.ai_banking_agent.exception;

/**
 * Thrown for client-side validation failures (blank message, empty query).
 * Mapped to HTTP 400 by {@code GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
