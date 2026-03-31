package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.helper.QuerySignal;

public interface SignalExtractorService {

    QuerySignal signalExtractor(String message);
}
