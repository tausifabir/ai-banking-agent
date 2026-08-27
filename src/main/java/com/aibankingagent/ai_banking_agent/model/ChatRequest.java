package com.aibankingagent.ai_banking_agent.model;

/**
 * Inbound chat request. Either {@code message} is required (text flow) or
 * {@code file} is provided (upload flow). Validation is enforced by the
 * controller with {@code @Valid}.
 */
public class ChatRequest {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
