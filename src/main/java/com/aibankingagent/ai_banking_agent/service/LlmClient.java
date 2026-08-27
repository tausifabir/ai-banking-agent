package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.tool.LlmMessage;
import com.aibankingagent.ai_banking_agent.tool.LlmResponse;
import com.aibankingagent.ai_banking_agent.tool.LlmToolDefinition;

import java.util.List;

/**
 * Boundary between the agent and whatever LLM provider is wired in. The
 * single method here always speaks the Ollama {@code /api/chat}
 * messages-and-tools protocol.
 */
public interface LlmClient {

    /**
     * Send {@code messages} to the model with {@code tools} available for
     * function calling. Returns the parsed assistant reply.
     */
    LlmResponse callWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools);
}
