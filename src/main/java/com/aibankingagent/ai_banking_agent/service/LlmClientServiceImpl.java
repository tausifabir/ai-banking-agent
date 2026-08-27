package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.exception.LlmUnavailableException;
import com.aibankingagent.ai_banking_agent.tool.LlmMessage;
import com.aibankingagent.ai_banking_agent.tool.LlmResponse;
import com.aibankingagent.ai_banking_agent.tool.LlmToolDefinition;
import com.aibankingagent.ai_banking_agent.tool.ToolCall;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Ollama-backed implementation of {@link LlmClient}. Speaks the
 * {@code /api/chat} protocol with {@code messages[]} + {@code tools[]}
 * and parses the assistant reply — including {@code tool_calls} — into
 * a structured {@link LlmResponse}.
 *
 * <p>Two layered timeouts guard the call so a hung Ollama process
 * doesn't make the user wait a full minute:
 *
 * <ul>
 *   <li>{@code banking.llm.connect-timeout-seconds} — TCP connect
 *       timeout (default 10s). If Ollama isn't listening, this trips
 *       first.</li>
 *   <li>{@code banking.llm.timeout-seconds} — full response timeout
 *       (default 15s). Applied both to the {@link HttpClient} (so the
 *       connection is cancelled at the network layer) and as a backstop
 *       to {@code .block()} (so a stuck scheduler thread still
 *       returns).</li>
 * </ul>
 *
 * <p>Exceptions are translated into a {@link LlmUnavailableException}
 * carrying a human-readable message — {@code ToolExecutor} surfaces
 * that string directly to the UI.
 */
@Service
public class LlmClientServiceImpl implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClientServiceImpl.class);

    private final WebClient webClient;
    private final String model;
    private final int responseTimeoutSeconds;

    public LlmClientServiceImpl(
            @Value("${banking.llm.base-url:http://localhost:11434}") String baseUrl,
            @Value("${banking.llm.model:llama3.1}") String model,
            @Value("${banking.llm.timeout-seconds:15}") int responseTimeoutSeconds,
            @Value("${banking.llm.connect-timeout-seconds:10}") int connectTimeoutSeconds) {

        Duration responseTimeout = Duration.ofSeconds(responseTimeoutSeconds);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) Duration.ofSeconds(connectTimeoutSeconds).toMillis())
                .responseTimeout(responseTimeout);

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.model = model;
        this.responseTimeoutSeconds = responseTimeoutSeconds;
        log.info("LlmClient configured: baseUrl={} model={} responseTimeout={}s connectTimeout={}s",
                baseUrl, model, responseTimeoutSeconds, connectTimeoutSeconds);
    }

    @Override
    public LlmResponse callWithTools(List<LlmMessage> messages,
                                     List<LlmToolDefinition> tools) {
        Objects.requireNonNull(messages, "messages must not be null");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("messages", serialise(messages));
        request.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolPayload = new ArrayList<>(tools.size());
            for (LlmToolDefinition t : tools) {
                toolPayload.add(t.toOllamaMap());
            }
            request.put("tools", toolPayload);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(responseTimeoutSeconds));
            return parse(response);
        } catch (WebClientResponseException e) {
            // HTTP error from Ollama (4xx/5xx).
            throw new LlmUnavailableException(
                    "AI service returned HTTP " + e.getStatusCode().value()
                            + ". Please try again later.",
                    e);
        } catch (WebClientRequestException e) {
            // Network-level failure: connect refused, DNS, timeout, etc.
            throw new LlmUnavailableException(friendlyNetworkMessage(e), e);
        } catch (IllegalStateException e) {
            // Reactor's "Timeout on blocking read" lands here when the
            // .block(Duration) backstop trips before the HttpClient timeout.
            if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
                throw new LlmUnavailableException(
                        "The AI service took too long to respond (>"
                                + responseTimeoutSeconds + "s). Please try a "
                                + "simpler question or try again later.",
                        e);
            }
            throw new LlmUnavailableException(
                    "AI service is unavailable. Please try again later.", e);
        } catch (RuntimeException e) {
            throw new LlmUnavailableException(
                    "AI service is unavailable. Please try again later.", e);
        }
    }

    /**
     * Map a {@link WebClientRequestException}'s underlying cause to a
     * user-readable string. We deliberately do NOT include the raw cause
     * class name — {@code ConnectException} and {@code TimeoutException}
     * are technical noise.
     */
    private String friendlyNetworkMessage(WebClientRequestException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ConnectException
                || (cause != null && cause.getMessage() != null
                && cause.getMessage().toLowerCase().contains("connection refused"))) {
            return "Cannot reach the AI service. Please check that the Ollama "
                    + "server is running and try again.";
        }
        if (cause instanceof TimeoutException
                || (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout"))) {
            return "The AI service took too long to respond (>"
                    + responseTimeoutSeconds + "s). Please try a simpler "
                    + "question or try again later.";
        }
        return "Cannot reach the AI service (" + e.getMessage()
                + "). Please try again later.";
    }

    /**
     * Convert our {@link LlmMessage} records into the wire shape Ollama
     * expects. We send {@code tool_calls} and {@code tool_name} only on
     * the roles that use them.
     */
    private List<Map<String, Object>> serialise(List<LlmMessage> messages) {
        List<Map<String, Object>> out = new ArrayList<>(messages.size());
        for (LlmMessage m : messages) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", m.role());
            entry.put("content", m.content() == null ? "" : m.content());
            if (m.toolCalls() != null && !m.toolCalls().isEmpty()) {
                List<Map<String, Object>> calls = new ArrayList<>(m.toolCalls().size());
                for (ToolCall tc : m.toolCalls()) {
                    calls.add(Map.of(
                            "function", Map.of(
                                    "name", tc.name(),
                                    "arguments", tc.arguments() == null
                                            ? Map.of()
                                            : tc.arguments())));
                }
                entry.put("tool_calls", calls);
            }
            if (m.toolName() != null) {
                entry.put("tool_name", m.toolName());
            }
            out.add(entry);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parse(Map<String, Object> response) {
        if (response == null) {
            return new LlmResponse("", List.of());
        }
        Object messageObj = response.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            log.warn("LLM response had no 'message' field: {}", response.keySet());
            return new LlmResponse("", List.of());
        }
        Map<String, Object> message = (Map<String, Object>) messageMap;

        String text = Objects.toString(message.get("content"), "").trim();

        List<ToolCall> toolCalls = new ArrayList<>();
        Object callsObj = message.get("tool_calls");
        if (callsObj instanceof List<?> rawList) {
            for (Object entry : rawList) {
                if (entry instanceof Map<?, ?> callMap) {
                    ToolCall parsed = parseCall((Map<String, Object>) callMap);
                    if (parsed != null) toolCalls.add(parsed);
                }
            }
        }

        log.debug("LLM response: textLen={} toolCalls={}", text.length(), toolCalls.size());
        return new LlmResponse(text, toolCalls);
    }

    @SuppressWarnings("unchecked")
    private ToolCall parseCall(Map<String, Object> call) {
        Object fnObj = call.get("function");
        if (!(fnObj instanceof Map<?, ?> fnMap)) return null;
        Map<String, Object> fn = (Map<String, Object>) fnMap;
        String name = Objects.toString(fn.get("name"), "");
        if (name.isBlank()) return null;
        Object argsObj = fn.get("arguments");
        Map<String, Object> args = (argsObj instanceof Map<?, ?> am)
                ? (Map<String, Object>) am
                : Map.of();
        return new ToolCall(name, args);
    }
}
