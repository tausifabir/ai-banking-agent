package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;

import java.util.List;

public interface RetrieverService {

    List<LoanScheme> retrieve(QuerySignal s, String message);

}
