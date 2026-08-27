package com.aibankingagent.ai_banking_agent.exception;

/**
 * Thrown when the LLM call fails (timeout, connection refused, malformed
 * response). Mapped to HTTP 503 by {@code GlobalExceptionHandler}.
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
