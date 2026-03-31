package com.aibankingagent.ai_banking_agent.service;

public interface AiService {

    String detectIntent(String message);

    String generateResponse(String message);
}
