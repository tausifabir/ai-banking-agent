package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.DocumentChunk;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import com.aibankingagent.ai_banking_agent.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PdfRetrieverService implements PdfRetriever {

    private final DocumentChunkRepository documentChunkRepository;

    public PdfRetrieverService(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    @Override
    public List<String> retrieveFromChunks(List<String> chunks, String message, QuerySignal s) {

        String msg = message.toLowerCase();

        List<String> dbChunks = documentChunkRepository.findAll()
                .stream()
                .map(DocumentChunk::getContent)
                .filter(Objects::nonNull)
                .toList();

        // 🔥 2. Merge runtime + DB chunks
        List<String> allChunks = new ArrayList<>();
        if (chunks != null) {
            allChunks.addAll(chunks);
        }
        allChunks.addAll(dbChunks);

        return allChunks.stream()
                .map(c -> Map.entry(c, score(c, msg, s)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private int score(String chunk, String msg, QuerySignal s) {


        if (chunk == null || msg == null) return 0;

        String c = chunk.toLowerCase();
        int score = 0;

        // 🔹 keyword match
        for (String word : msg.split("\\s+")) {
            if (!word.isBlank() && c.contains(word)) {
                score++;
            }
        }

        // 🔥 signal boost
        if (s != null) {
            if (s.isLoan() && c.contains("loan")) score += 3;
            if (s.isAccount() && c.contains("account")) score += 3;
            if (s.isInterest() && c.contains("interest")) score += 3;

            // 🔥 purpose boost
            if (s.getPurpose() != null && c.contains(s.getPurpose())) {
                score += 4;
            }
        }

        return score;
    }
}
