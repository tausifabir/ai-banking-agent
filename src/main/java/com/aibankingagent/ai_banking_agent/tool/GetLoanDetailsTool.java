package com.aibankingagent.ai_banking_agent.tool;

import com.aibankingagent.ai_banking_agent.service.LoanService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Looks up a single loan by (case-insensitive, partial) name and returns
 * its full details. Thin wrapper over {@link LoanService#getLoanDetails}.
 */
@Component
public class GetLoanDetailsTool implements Tool {

    private final LoanService loanService;

    public GetLoanDetailsTool(LoanService loanService) {
        this.loanService = loanService;
    }

    @Override
    public String name() {
        return "get_loan_details";
    }

    @Override
    public String description() {
        return "Look up full details for one loan scheme by name (e.g. \"Personal Loan\", \"Home Loan\", \"SME Loan\"). Returns min/max amount, interest rate, target group and description.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "loan_name", Map.of(
                                "type", "string",
                                "description", "The loan scheme name to look up. Partial matches are accepted.")),
                "required", java.util.List.of("loan_name"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        Object raw = arguments == null ? null : arguments.get("loan_name");
        if (raw == null || raw.toString().isBlank()) {
            return "Error: 'loan_name' argument is required.";
        }
        return loanService.getLoanDetails(raw.toString());
    }
}
