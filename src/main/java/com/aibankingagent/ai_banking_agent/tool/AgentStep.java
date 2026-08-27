package com.aibankingagent.ai_banking_agent.tool;

import java.util.List;

/**
 * One iteration of the agent loop. {@code thinking} is any text the
 * model emitted alongside its tool calls (or instead of them); it's the
 * "thinking out loud" you'd want to show in the UI. {@code toolInvocations}
 * lists each tool the model called in this iteration, paired with the
 * string the executor returned.
 */
public record AgentStep(int iteration,
                        String thinking,
                        List<ToolInvocation> toolInvocations) {
}
