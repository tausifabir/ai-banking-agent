package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.helper.QuerySignal;

import java.util.List;

public interface PdfRetriever {

    List<String> retrieveFromChunks(List<String> chunks, String message, QuerySignal s);

}
