package com.aibankingagent.ai_banking_agent.service;

import org.springframework.stereotype.Service;

@Service
public class BankInfoServiceImpl implements BankInfoService{


    @Override
    public String getInfo(String intent) {
        return switch (intent) {
            case "LOAN_INFO" -> "We offer personal, home, and car loans. Interest starts from 9% per year.";
            case "ACCOUNT_OPEN" -> "To open an account, you need NID, passport-size photo, and minimum deposit.";
            case "INTEREST_RATE" -> "Savings account interest rate is approximately 4% per year.";
            default -> "Sorry, I can only help with general banking information.";
        };
    }
}
