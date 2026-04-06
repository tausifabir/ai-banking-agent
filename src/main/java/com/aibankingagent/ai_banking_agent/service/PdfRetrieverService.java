package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PdfRetrieverService implements PdfRetriever {
    @Override
    public List<String> retrieveFromChunks(List<String> chunks, String message, QuerySignal s) {
        String msg = message.toLowerCase();

        return chunks.stream()
                .map(c -> Map.entry(c, score(c, msg, s)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private int score(String chunk, String msg, QuerySignal s) {

        String c = chunk.toLowerCase();
        int score = 0;

        // 🔹 keyword match
        for (String word : msg.split(" ")) {
            if (c.contains(word)) score++;
        }

        // 🔥 signal boost
        if (s.isLoan() && c.contains("loan")) score += 3;
        if (s.isAccount() && c.contains("account")) score += 3;
        if (s.isInterest() && c.contains("interest")) score += 3;

        // 🔥 purpose boost
        if (s.getPurpose() != null && c.contains(s.getPurpose())) {
            score += 4;
        }

        return score;
    }
}
