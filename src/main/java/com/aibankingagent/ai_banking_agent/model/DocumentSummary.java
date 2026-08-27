package com.aibankingagent.ai_banking_agent.model;

/**
 * Summary of an uploaded document, grouped by source filename.
 */
public class DocumentSummary {

    private String source;
    private String type;
    private long chunkCount;

    public DocumentSummary() {
    }

    public DocumentSummary(String source, String type, long chunkCount) {
        this.source = source;
        this.type = type;
        this.chunkCount = chunkCount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(long chunkCount) {
        this.chunkCount = chunkCount;
    }
}
