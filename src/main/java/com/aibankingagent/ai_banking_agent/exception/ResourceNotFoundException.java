package com.aibankingagent.ai_banking_agent.exception;

/**
 * Thrown when a resource (loan, document) cannot be found.
 * Mapped to HTTP 404 by {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
