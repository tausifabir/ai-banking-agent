package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;

import java.util.List;

public interface ContextBuilder {

    String buildContext(List<LoanScheme> loans);
}
