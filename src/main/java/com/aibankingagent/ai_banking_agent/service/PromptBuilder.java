package com.aibankingagent.ai_banking_agent.service;

public interface PromptBuilder {
    String build(String context, String question);
}
