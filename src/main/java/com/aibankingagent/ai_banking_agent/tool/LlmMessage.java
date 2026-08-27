package com.aibankingagent.ai_banking_agent.tool;

import java.util.List;

/**
 * One entry in the {@code messages[]} array sent to Ollama's
 * {@code /api/chat}. Roles supported here:
 *
 * <ul>
 *   <li>{@code system} — system prompt; {@code content} populated,
 *       {@code toolCalls} and {@code toolName} null</li>
 *   <li>{@code user} — user prompt; same shape as system</li>
 *   <li>{@code assistant} — model reply; {@code content} may be empty
 *       when the model only emitted tool calls; {@code toolCalls}
 *       populated when the model invoked functions</li>
 *   <li>{@code tool} — function result fed back to the model;
 *       {@code toolName} populated with the executed function's name</li>
 * </ul>
 */
public record LlmMessage(String role,
                         String content,
                         List<ToolCall> toolCalls,
                         String toolName) {

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null);
    }

    public static LlmMessage assistant(String content, List<ToolCall> toolCalls) {
        return new LlmMessage("assistant", content, toolCalls, null);
    }

    public static LlmMessage toolResult(String toolName, String content) {
        return new LlmMessage("tool", content, null, toolName);
    }
}
