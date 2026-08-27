package com.aibankingagent.ai_banking_agent.exception;

/**
 * Thrown when an upload's content type is not in the allowed MIME set.
 * Mapped to HTTP 415 by {@code GlobalExceptionHandler}.
 */
public class UnsupportedFileTypeException extends RuntimeException {

    private final String contentType;

    public UnsupportedFileTypeException(String contentType) {
        super("Unsupported file type: " + contentType);
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
