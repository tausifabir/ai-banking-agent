package com.aibankingagent.ai_banking_agent.tool;

import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import com.aibankingagent.ai_banking_agent.service.PdfRetriever;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Retrieves the top document chunks relevant to a free-text query. Wraps
 * {@link PdfRetriever#retrieveFromChunks} — the same call the
 * orchestrator uses for the upload flow — so a tool invocation gets the
 * same ranking logic.
 */
@Component
public class SearchDocumentsTool implements Tool {

    private final PdfRetriever pdfRetriever;

    public SearchDocumentsTool(PdfRetriever pdfRetriever) {
        this.pdfRetriever = pdfRetriever;
    }

    @Override
    public String name() {
        return "search_documents";
    }

    @Override
    public String description() {
        return "Search the persisted document corpus for chunks relevant to the query. Use this when the user asks a question that may be answered by an uploaded document (PDF or image). Returns the top matching snippets.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "Free-text search query.")),
                "required", java.util.List.of("query"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        if (arguments == null) {
            return "Error: arguments were null.";
        }
        Object raw = arguments.get("query");
        if (raw == null || raw.toString().isBlank()) {
            return "Error: 'query' argument is required.";
        }
        String query = raw.toString();

        // Reuse the signal-extraction + retriever ranking by building a
        // minimal signal. We don't expose purpose/amount here — the
        // retriever still scores by keyword overlap.
        QuerySignal signal = new QuerySignal();
        List<String> chunks = pdfRetriever.retrieveFromChunks(
                Collections.emptyList(), query, signal);

        if (chunks.isEmpty()) {
            return "No relevant document chunks found.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String snippet = chunk.length() <= 200 ? chunk : chunk.substring(0, 200);
            sb.append("[").append(i + 1).append("] ").append(snippet).append("\n");
        }
        return sb.toString();
    }
}
