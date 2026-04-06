package com.aibankingagent.ai_banking_agent.service;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    String extract(MultipartFile file);
}
