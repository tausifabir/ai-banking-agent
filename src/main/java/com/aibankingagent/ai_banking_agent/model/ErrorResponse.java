package com.aibankingagent.ai_banking_agent.model;

import java.time.Instant;

/**
 * Stable JSON shape for every error response. Frontend renders
 * {@code message}; ops correlates via {@code requestId}.
 */
public class ErrorResponse {

    private String code;
    private String message;
    private Instant timestamp;
    private String requestId;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, String requestId) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
