package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.DocumentChunk;
import com.aibankingagent.ai_banking_agent.tool.AgentResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiService {

    /**
     * Answer a plain-text banking question. Returns the full agent trace
     * (final answer + ordered steps + optional error) so the caller can
     * render intermediate state.
     */
    AgentResult generateResponse(String message);

    /**
     * Answer a banking question that accompanies an uploaded file.
     */
    AgentResult processFile(String message, MultipartFile file);

    /**
     * Extract text from the uploaded file, chunk it, and persist the
     * chunks. Used by the upload-only endpoint when no question accompanies
     * the file.
     */
    List<DocumentChunk> ingestFile(MultipartFile file);
}
