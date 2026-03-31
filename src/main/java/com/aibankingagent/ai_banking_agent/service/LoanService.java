package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;

import java.util.List;

public interface LoanService {
    String getLoanDetails(String name);
    List<LoanScheme> getAllLoans();
    String getLoanSummary();
}
