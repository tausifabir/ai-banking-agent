package com.aibankingagent.ai_banking_agent.tool;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.repository.LoanSchemeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Finds loan schemes whose declared amount range covers the requested
 * amount. Schemes with {@code null} bounds are treated as open-ended on
 * that side (so a Personal Loan with {@code maxAmount = null} matches
 * any amount up to its {@code minAmount}).
 */
@Component
public class FindLoansByAmountTool implements Tool {

    private final LoanSchemeRepository repository;

    public FindLoansByAmountTool(LoanSchemeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "find_loans_by_amount";
    }

    @Override
    public String description() {
        return "Find loan schemes whose min/max amount range covers the given amount (in BDT). Schemes with a null min or max bound are treated as open-ended on that side.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "amount", Map.of(
                                "type", "number",
                                "description", "The amount in BDT the user wants to borrow.")),
                "required", java.util.List.of("amount"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        if (arguments == null) {
            return "Error: arguments were null.";
        }
        Object raw = arguments.get("amount");
        if (raw == null) {
            return "Error: 'amount' argument is required.";
        }
        double amount;
        try {
            amount = Double.parseDouble(raw.toString());
        } catch (NumberFormatException e) {
            return "Error: 'amount' must be a number.";
        }

        List<LoanScheme> matched = repository.findAll().stream()
                .filter(l -> covers(l, amount))
                .toList();

        if (matched.isEmpty()) {
            return "No loan scheme covers amount " + amount + " BDT.";
        }

        return matched.stream()
                .map(l -> String.format("- %s | %s-%s BDT | %s | %s",
                        l.getName(),
                        l.getMinAmount() == null ? "0" : String.valueOf(l.getMinAmount().longValue()),
                        l.getMaxAmount() == null ? "open" : String.valueOf(l.getMaxAmount().longValue()),
                        l.getInterestRate() == null ? "n/a" : l.getInterestRate(),
                        l.getTargetGroup() == null ? "general" : l.getTargetGroup()))
                .collect(Collectors.joining("\n"));
    }

    private boolean covers(LoanScheme l, double amount) {
        double min = l.getMinAmount() == null ? 0.0 : l.getMinAmount();
        double max = l.getMaxAmount() == null ? Double.POSITIVE_INFINITY : l.getMaxAmount();
        return amount >= min && amount <= max;
    }
}
