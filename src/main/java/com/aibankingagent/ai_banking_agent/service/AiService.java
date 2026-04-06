package com.aibankingagent.ai_banking_agent.service;

import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    String generateResponse(String message);
    String processFile(String message, MultipartFile file);
}
