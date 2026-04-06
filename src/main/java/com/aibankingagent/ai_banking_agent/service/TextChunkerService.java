package com.aibankingagent.ai_banking_agent.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkerService implements TextChunker {

    private static final int CHUNK_SIZE = 500;     // characters
    private static final int OVERLAP = 100;

    @Override
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        text = clean(text);

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(start + CHUNK_SIZE, text.length());

            String chunk = text.substring(start, end);

            chunks.add(chunk.trim());

            start += (CHUNK_SIZE - OVERLAP);
        }

        return chunks;
    }


    // 🧹 Clean noisy PDF text
    private String clean(String text) {

        return text
                .replaceAll("\\s+", " ")     // remove extra spaces
                .replaceAll("\\n+", "\n")   // normalize new lines
                .trim();
    }
}
