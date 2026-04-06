package com.aibankingagent.ai_banking_agent.service;

import java.util.List;

public interface TextChunker {
    List<String> chunk(String text);
}
