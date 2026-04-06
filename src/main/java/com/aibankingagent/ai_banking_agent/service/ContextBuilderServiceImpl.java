package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContextBuilderServiceImpl implements ContextBuilder {

    @Override
    public String buildContext(List<LoanScheme> loans) {
        return loans.stream()
                .map(this::format)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String buildFromPdf(List<String> chunks) {
        return chunks.stream()
                .map(this::cleanChunk)
                .collect(Collectors.joining("\n---\n"));
    }

    private String format(LoanScheme l) {
        return String.format(
                "%s | %s-%s BDT | %s | %s",
                l.getName(),
                val(l.getMinAmount()),
                val(l.getMaxAmount()),
                l.getInterestRate(),
                shortText(l.getDescription())
        );
    }

    private String val(Double v) {
        return v != null ? String.valueOf(v.longValue()) : "-";
    }

    private String shortText(String d) {
        if (d == null) return "";
        return d.length() > 40 ? d.substring(0, 40) + "..." : d;
    }

    private String cleanChunk(String chunk) {

        if (chunk == null) return "";

        return chunk
                .replaceAll("\\s+", " ")   // remove noise
                .trim()
                .substring(0, Math.min(chunk.length(), 300)); // limit size 🔥
    }

}
