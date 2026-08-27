package com.aibankingagent.ai_banking_agent.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Outbound chat response.
 *
 * <ul>
 *   <li>{@code answer} — the final user-facing text (may be empty if
 *       {@code error} is set)</li>
 *   <li>{@code intent} — classifier label or flow marker
 *       (LOAN_INFO / ACCOUNT_OPEN / INTEREST_RATE / RAG / FILE_QA /
 *       BLOCKED / LOAN_DETAILS)</li>
 *   <li>{@code sources} — source filenames or labels referenced in the
 *       answer</li>
 *   <li>{@code requestId} — the X-Request-Id for correlation</li>
 *   <li>{@code error} — populated when the agent run failed (LLM call
 *       failed mid-loop, etc.); partial steps are still returned</li>
 *   <li>{@code steps} — every iteration the agent went through, in order,
 *       with the model's thinking text and each tool invocation + result</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String answer;
    private String intent;
    private List<String> sources;
    private String requestId;
    private String error;
    private List<AgentStepView> steps;

    public ChatResponse(String answer,
                        String intent,
                        List<String> sources,
                        String requestId) {
        this(answer, intent, sources, requestId, null, null);
    }

    /**
     * One iteration of the agent loop as the UI sees it.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStepView {
        private int iteration;
        private String thinking;
        private List<ToolInvocationView> toolCalls;
    }

    /**
     * One tool call the model made, paired with the string the executor
     * returned. {@code arguments} is the raw map the model emitted so
     * the UI can render it as-is.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolInvocationView {
        private String name;
        private Map<String, Object> arguments;
        private String result;
    }
}
