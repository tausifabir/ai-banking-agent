package com.aibankingagent.ai_banking_agent.tool;

import java.util.Map;

/**
 * One entry in the {@code tools} array sent to Ollama. Mirrors the
 * OpenAI-compatible function schema that Ollama accepts.
 */
public record LlmToolDefinition(String name,
                                String description,
                                Map<String, Object> parameters) {

    /**
     * Serialise to the {@code {"type":"function","function":{...}}}
     * shape Ollama expects.
     */
    public Map<String, Object> toOllamaMap() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters));
    }
}
