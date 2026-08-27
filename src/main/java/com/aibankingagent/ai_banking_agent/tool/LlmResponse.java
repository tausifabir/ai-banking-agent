package com.aibankingagent.ai_banking_agent.tool;

import java.util.List;

/**
 * Parsed reply from a single Ollama {@code /api/chat} call. Either
 * {@code text} is populated (model answered directly) or
 * {@code toolCalls} is non-empty (model wants to invoke one or more
 * functions). It is possible for both to be populated — the model may
 * emit a short partial sentence plus tool calls.
 */
public record LlmResponse(String text, List<ToolCall> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }
}
