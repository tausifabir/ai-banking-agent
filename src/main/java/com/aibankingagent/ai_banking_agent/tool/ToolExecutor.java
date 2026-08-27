package com.aibankingagent.ai_banking_agent.tool;

import com.aibankingagent.ai_banking_agent.service.LlmClient;
import com.aibankingagent.ai_banking_agent.service.ResponseValidatorServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Drives the agent loop:
 *
 * <pre>
 *   send messages + tools
 *     → if model returns text, finish
 *     → if model returns tool_calls, execute them, append results, repeat
 * </pre>
 *
 * <p>Bounded by two limits:
 * <ul>
 *   <li>{@code banking.agent.max-iterations} (default 3) — caps how many
 *       model round-trips the loop will make.</li>
 *   <li>{@code banking.agent.total-budget-seconds} (default 60) — caps
 *       total wall-clock time. Before each LLM call we check elapsed
 *       time; if the budget is exceeded we abort with an
 *       {@code AgentResult.error(...)} so the user sees a clear message
 *       instead of the loop hanging on a slow model.</li>
 * </ul>
 *
 * <p>Returns an {@link AgentResult} carrying the final answer, every
 * iteration's thinking + tool calls, and an optional error if an LLM
 * call failed mid-loop or the budget was exhausted. Partial state is
 * preserved so the UI can show what the agent did before failing.
 */
@Service
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);
    private static final String LOOP_EXHAUSTED =
            "Sorry, I wasn't able to finish that in a reasonable number of steps.";

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ResponseValidatorServiceImpl responseValidator;
    private final int maxIterations;
    private final long budgetMs;

    public ToolExecutor(LlmClient llmClient,
                        ToolRegistry toolRegistry,
                        ResponseValidatorServiceImpl responseValidator,
                        @Value("${banking.agent.max-iterations:3}") int maxIterations,
                        @Value("${banking.agent.total-budget-seconds:60}") int totalBudgetSeconds) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.responseValidator = responseValidator;
        this.maxIterations = Math.max(1, maxIterations);
        this.budgetMs = TimeUnit.SECONDS.toMillis(Math.max(1, totalBudgetSeconds));
    }

    /**
     * Run the loop using every tool registered with {@link ToolRegistry}.
     */
    public AgentResult runLoop(List<LlmMessage> messages) {
        return runLoop(messages, toolRegistry.definitions());
    }

    /**
     * Run the loop with an explicit tool set (mostly for tests).
     */
    public AgentResult runLoop(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        long loopStartNs = System.nanoTime();
        log.debug("Tool loop start: tools={} initialMessages={} budgetMs={}",
                tools.size(), messages.size(), budgetMs);

        List<AgentStep> steps = new ArrayList<>();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            long iterStartNs = System.nanoTime();

            // Budget check before we spend another LLM round-trip.
            long elapsed = elapsedMs(loopStartNs);
            if (elapsed >= budgetMs) {
                log.warn("Tool loop aborting at iteration {}: budget exhausted "
                        + "(elapsedMs={} budgetMs={})", iteration, elapsed, budgetMs);
                return AgentResult.error(
                        "The agent run exceeded the time budget of "
                                + TimeUnit.MILLISECONDS.toSeconds(budgetMs)
                                + "s. Please try a simpler question or try again later.",
                        steps);
            }

            LlmResponse response;
            try {
                response = llmClient.callWithTools(messages, tools);
            } catch (Exception e) {
                log.warn("LLM call failed at iteration {} after {}ms: {}",
                        iteration, elapsedMs(iterStartNs), e.toString());
                return AgentResult.error(
                        "LLM call failed: " + friendlyMessage(e),
                        steps);
            }

            long llmMs = elapsedMs(iterStartNs);
            log.info("LLM iteration {} done in {}ms (textLen={} toolCalls={})",
                    iteration, llmMs,
                    response.text() == null ? 0 : response.text().length(),
                    response.toolCalls() == null ? 0 : response.toolCalls().size());

            if (!response.hasToolCalls()) {
                String text = response.hasText() ? response.text() : "";
                String validated = responseValidator.validate(text);
                log.debug("Tool loop finished after {} iteration(s); totalMs={}",
                        iteration, elapsedMs(loopStartNs));
                return AgentResult.of(validated, steps);
            }

            // Execute each call, collect invocations, append tool messages.
            List<ToolCall> calls = response.toolCalls() == null
                    ? List.of() : response.toolCalls();
            List<ToolInvocation> invocations = new ArrayList<>(calls.size());
            messages.add(LlmMessage.assistant(
                    response.hasText() ? response.text() : "",
                    calls));

            for (ToolCall call : calls) {
                long toolStartNs = System.nanoTime();
                ToolInvocation invocation = executeOne(call, iteration);
                log.info("Tool '{}' completed in {}ms",
                        call.name(), elapsedMs(toolStartNs));
                invocations.add(invocation);
                messages.add(LlmMessage.toolResult(call.name(), invocation.result()));
            }

            steps.add(new AgentStep(
                    iteration,
                    response.hasText() ? response.text() : "",
                    invocations));
        }

        log.warn("Tool loop hit max iterations ({}) without a final answer "
                + "(totalMs={})", maxIterations, elapsedMs(loopStartNs));
        return AgentResult.of(LOOP_EXHAUSTED, steps);
    }

    /**
     * Execute a single tool call. Exceptions are caught and turned into
     * an error string that is fed back to the model and shown in the UI.
     */
    private ToolInvocation executeOne(ToolCall call, int iteration) {
        Tool tool = toolRegistry.get(call.name());
        if (tool == null) {
            log.warn("Iteration {}: unknown tool '{}'", iteration, call.name());
            String msg = "Error: tool '" + call.name() + "' is not registered.";
            return new ToolInvocation(call.name(), call.arguments(), msg);
        }

        log.info("Iteration {}: calling tool '{}' args={}",
                iteration, call.name(), call.arguments());

        try {
            String result = tool.execute(call.arguments());
            return new ToolInvocation(call.name(), call.arguments(), result);
        } catch (Exception e) {
            log.warn("Iteration {}: tool '{}' threw {}",
                    iteration, call.name(), e.toString());
            String msg = "Error: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage());
            return new ToolInvocation(call.name(), call.arguments(), msg);
        }
    }

    /**
     * Reduce an exception thrown by the LLM client to a one-line
     * message. The Ollama-backed client already returns friendly
     * strings inside its thrown exception, so this is mostly a defensive
     * backstop for any other cause.
     */
    private static String friendlyMessage(Throwable e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getMessage();
    }

    private static long elapsedMs(long startNs) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
    }
}
