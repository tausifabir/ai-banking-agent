package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.tool.LlmMessage;

import java.util.List;

public interface PromptBuilder {

    /**
     * Build the initial {@code messages[]} array (system + user) for the
     * LLM. The system message carries persistent rules; the user message
     * carries the assembled context plus the user's question.
     */
    List<LlmMessage> build(String context, String question);
}
