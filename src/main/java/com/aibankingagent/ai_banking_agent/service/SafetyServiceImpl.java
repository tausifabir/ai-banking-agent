package com.aibankingagent.ai_banking_agent.service;

import org.springframework.stereotype.Service;

@Service
public class SafetyServiceImpl implements SafetyService {
    @Override
    public boolean isSensitive(String message) {

        message = message.toLowerCase();

        return message.contains("balance") ||
                message.contains("account number") ||
                message.contains("routing") ||
                message.contains("transaction") ||
                message.contains("my account");
    }
}
