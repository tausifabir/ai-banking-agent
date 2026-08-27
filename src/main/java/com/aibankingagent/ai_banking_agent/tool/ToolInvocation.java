package com.aibankingagent.ai_banking_agent.tool;

import java.util.Map;

/**
 * One tool call paired with the string its executor returned. The
 * arguments are kept as a raw map so the UI can show them as-is without
 * losing structure (e.g. {@code {principal: 1000000, tenure_months: 36}}).
 */
public record ToolInvocation(String name,
                             Map<String, Object> arguments,
                             String result) {
}
