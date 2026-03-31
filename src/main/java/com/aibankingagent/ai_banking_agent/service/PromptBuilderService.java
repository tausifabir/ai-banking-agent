package com.aibankingagent.ai_banking_agent.service;

import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService implements PromptBuilder {
    @Override
    public String build(String context, String question) {
        return """
                            You are a banking assistant.
                            Answer the user's question ONLY using the provided data.
                            Do NOT make up information.
                            If answer is not found, say: "Sorry, I don't have that information."
                            ---------------------
                            Available Loan Data:
                            %s
                            ---------------------
                            User Question:
                            %s
                            ---------------------
                            RULES:
                            - Do NOT add extra info
                            - If not found say: NOT_FOUND
                            ---------------------
                            Give a clear and helpful answer.
                        """.formatted(context, question);
    }
}
