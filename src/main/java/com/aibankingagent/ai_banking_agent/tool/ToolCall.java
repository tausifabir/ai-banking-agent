package com.aibankingagent.ai_banking_agent.tool;

import java.util.Map;

/**
 * A single function invocation the model requested. {@code arguments} is
 * already a parsed map — Ollama sends it as an object, not a stringified
 * JSON blob.
 */
public record ToolCall(String name, Map<String, Object> arguments) {
}
