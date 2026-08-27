package com.aibankingagent.ai_banking_agent.tool;

import java.util.Map;

/**
 * A function-callable capability exposed to the LLM. Implementations are
 * Spring {@code @Component}s; {@link ToolRegistry} picks them up by
 * {@link #name()}.
 *
 * <p>{@link #parametersSchema()} returns a JSON-schema-shaped map
 * (properties, required, type=object) that is serialised into the
 * {@code tools} array of the Ollama {@code /api/chat} request.
 *
 * <p>{@link #execute(Map)} receives the {@code arguments} object the
 * model emitted and returns a string result. Errors should be turned
 * into short, human-readable strings — the LLM sees them as the
 * {@code tool} role message and decides whether to retry.
 */
public interface Tool {

    String name();

    String description();

    /**
     * Schema as a Java map mirroring JSON Schema. Example:
     * <pre>
     * {
     *   "type": "object",
     *   "properties": {
     *     "loan_name": {"type": "string", "description": "..."}
     *   },
     *   "required": ["loan_name"]
     * }
     * </pre>
     */
    Map<String, Object> parametersSchema();

    /**
     * Run the tool with the model-supplied arguments and return its
     * result as a string. Returning a string keeps the wire shape simple
     * — the orchestrator will wrap whatever the tool produced into a
     * {@code tool}-role message and feed it back to the model.
     */
    String execute(Map<String, Object> arguments);
}
