package com.aibankingagent.ai_banking_agent.service;

import org.springframework.stereotype.Service;

@Service
public class ResponseValidatorService implements ResponseValidator {
    @Override
    public String validate(String response) {
        if (response == null || response.isBlank()) {
            return "Sorry, I don't have that information.";
        }

        if (response.contains("NOT_FOUND")) {
            return "Sorry, I don't have that information.";
        }

        return response;
    }
}
