package com.aibankingagent.ai_banking_agent.service;

import org.springframework.stereotype.Service;

@Service
public class IntentActionServiceImpl implements IntentActionService {

    private final LoanService loanService;

    public IntentActionServiceImpl(LoanService loanService) {
        this.loanService = loanService;
    }

    @Override
    public String execute(String intent) {
        return switch (intent) {
            case "LOAN_INFO" -> this.loanService.getLoanSummary();
            case "ACCOUNT_OPEN" -> "To open an account, you need NID, passport-size photo, and minimum deposit.";
            case "INTEREST_RATE" -> "Savings account interest rate is approximately 4% per year.";
            default -> "Sorry, I can only help with general banking information.";
        };
    }
}
