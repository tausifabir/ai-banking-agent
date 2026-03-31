package com.aibankingagent.ai_banking_agent.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class LlmClientService implements LlmClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .build();;

    @Override
    public String call(String prompt) {

        Map<String, Object> request = Map.of(
                "model", "llama3",
//                "model", "llama3.1",
//                "model", "dolphin3",
                "prompt", prompt,
                "stream", false
        );

        Map response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response.get("response").toString().trim();
    }
}
