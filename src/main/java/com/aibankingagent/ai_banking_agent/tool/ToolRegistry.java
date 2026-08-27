package com.aibankingagent.ai_banking_agent.tool;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects every {@link Tool} bean at startup and exposes lookup-by-name
 * plus a snapshot of {@link LlmToolDefinition}s ready to send to the LLM.
 *
 * <p>Use a {@link LinkedHashMap} so iteration order is deterministic
 * (helpful when reading logs).
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final List<Tool> tools;
    private final Map<String, Tool> byName = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools;
    }

    @PostConstruct
    void init() {
        for (Tool t : tools) {
            Tool previous = byName.put(t.name(), t);
            if (previous != null) {
                log.warn("Duplicate tool name '{}' — overwriting {} with {}",
                        t.name(), previous.getClass().getSimpleName(),
                        t.getClass().getSimpleName());
            }
        }
        log.info("Tool registry initialised with {} tool(s): {}",
                byName.size(), byName.keySet());
    }

    /**
     * Look up a tool by the name the model emitted. Returns {@code null}
     * if no such tool is registered (the executor should treat that as
     * an error message back to the model rather than throwing).
     */
    public Tool get(String name) {
        return byName.get(name);
    }

    /**
     * Snapshot of every registered tool, serialised into the
     * {@code tools[]} array shape Ollama expects.
     */
    public List<LlmToolDefinition> definitions() {
        return byName.values().stream()
                .map(t -> new LlmToolDefinition(
                        t.name(), t.description(), t.parametersSchema()))
                .toList();
    }

    public boolean isEmpty() {
        return byName.isEmpty();
    }

    public int size() {
        return byName.size();
    }
}
