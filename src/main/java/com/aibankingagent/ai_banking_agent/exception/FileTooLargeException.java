package com.aibankingagent.ai_banking_agent.exception;

/**
 * Thrown when an uploaded file exceeds the configured size limit.
 * Mapped to HTTP 413 by {@code GlobalExceptionHandler}.
 */
public class FileTooLargeException extends RuntimeException {

    private final long actualSize;
    private final long maxSize;

    public FileTooLargeException(long actualSize, long maxSize) {
        super("File size " + actualSize + " bytes exceeds the " + maxSize + " byte limit");
        this.actualSize = actualSize;
        this.maxSize = maxSize;
    }

    public long getActualSize() {
        return actualSize;
    }

    public long getMaxSize() {
        return maxSize;
    }
}
