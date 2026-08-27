package com.aibankingagent.ai_banking_agent.tool;

import java.util.List;

/**
 * Final result of an agent run. {@code finalAnswer} is the user-facing
 * string (or empty if the run errored before producing one).
 * {@code steps} lists every iteration the loop went through, in order —
 * each step carries the model's thinking text and the tool calls it
 * made plus their results. {@code error} is set when an LLM call failed
 * mid-loop; the partial {@code steps} are still populated so the UI can
 * show what the agent did before failing.
 */
public record AgentResult(String finalAnswer,
                          List<AgentStep> steps,
                          String error) {

    public static AgentResult of(String finalAnswer, List<AgentStep> steps) {
        return new AgentResult(finalAnswer, steps, null);
    }

    public static AgentResult error(String message, List<AgentStep> steps) {
        return new AgentResult("", steps, message);
    }

    public boolean hasError() {
        return error != null && !error.isBlank();
    }
}
