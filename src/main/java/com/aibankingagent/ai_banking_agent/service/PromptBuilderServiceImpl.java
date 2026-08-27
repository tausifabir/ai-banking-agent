package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.tool.LlmMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilderServiceImpl implements PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are a banking assistant for a commercial bank in Bangladesh.

            Rules:
            - Answer ONLY using the data in the conversation context (user-uploaded document chunks and loan schemes).
            - If the answer is not in the context, you MUST call a tool to look it up before answering.
            - Prefer calling tools over guessing. Use get_loan_summary when the user asks what loans exist; use get_loan_details when they ask about a specific loan; use find_loans_by_amount when they have a budget; use search_documents when the question is about an uploaded document; use calculate_emi when they ask about monthly payments.
            - Do NOT make up information. If a tool returns no match or the answer cannot be found, reply exactly: NOT_FOUND
            - Keep the final user-facing answer under 5 lines.
            - Currency is BDT unless the user says otherwise.
            """;

    @Override
    public List<LlmMessage> build(String context, String question) {
        String userBody;
        if (context == null || context.isBlank()) {
            userBody = question == null ? "" : question;
        } else {
            userBody = "AVAILABLE DATA:\n" + context + "\n\nUSER QUESTION:\n" + question;
        }
        return List.of(
                LlmMessage.system(SYSTEM_PROMPT),
                LlmMessage.user(userBody));
    }
}
