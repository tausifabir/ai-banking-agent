package com.aibankingagent.ai_banking_agent.tool;

import com.aibankingagent.ai_banking_agent.service.LoanService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Returns the catalogue of all available loan schemes (names only) so
 * the model can show the user what's on offer without inventing names.
 */
@Component
public class GetLoanSummaryTool implements Tool {

    private final LoanService loanService;

    public GetLoanSummaryTool(LoanService loanService) {
        this.loanService = loanService;
    }

    @Override
    public String name() {
        return "get_loan_summary";
    }

    @Override
    public String description() {
        return "List every loan scheme the bank currently offers. Returns loan names with a short description for each. Use this when the user asks what loans are available or wants a comparison.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        // No parameters — Ollama requires the schema to still be a valid
        // object, so we send an empty properties map.
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", java.util.List.of());
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        return loanService.getLoanSummary();
    }
}
