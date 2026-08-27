package com.aibankingagent.ai_banking_agent.controller;

import com.aibankingagent.ai_banking_agent.config.BankingProperties;
import com.aibankingagent.ai_banking_agent.config.RequestIdFilter;
import com.aibankingagent.ai_banking_agent.entity.DocumentChunk;
import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.exception.FileTooLargeException;
import com.aibankingagent.ai_banking_agent.exception.InvalidRequestException;
import com.aibankingagent.ai_banking_agent.exception.ResourceNotFoundException;
import com.aibankingagent.ai_banking_agent.exception.UnsupportedFileTypeException;
import com.aibankingagent.ai_banking_agent.model.ChatRequest;
import com.aibankingagent.ai_banking_agent.model.ChatResponse;
import com.aibankingagent.ai_banking_agent.model.DocumentSummary;
import com.aibankingagent.ai_banking_agent.model.LoanSummary;
import com.aibankingagent.ai_banking_agent.repository.DocumentChunkRepository;
import com.aibankingagent.ai_banking_agent.repository.LoanSchemeRepository;
import com.aibankingagent.ai_banking_agent.service.AiService;
import com.aibankingagent.ai_banking_agent.service.BankInfoService;
import com.aibankingagent.ai_banking_agent.service.LoanService;
import com.aibankingagent.ai_banking_agent.service.SafetyService;
import com.aibankingagent.ai_banking_agent.tool.AgentResult;
import com.aibankingagent.ai_banking_agent.tool.AgentStep;
import com.aibankingagent.ai_banking_agent.tool.ToolInvocation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HTTP entry point for the banking assistant. Responsibilities:
 *
 * <ul>
 *   <li>Validate inbound requests (file size, MIME type, message length)</li>
 *   <li>Short-circuit obvious intents (sidebar quick-buttons) without paying
 *       an LLM round-trip</li>
 *   <li>Expose the static loan catalogue and the persisted document corpus</li>
 *   <li>Delegate banking Q&amp;A to {@link AiService}</li>
 * </ul>
 *
 * Errors are translated into a stable JSON shape by
 * {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /**
     * Sidebar quick-button keywords. When the message is one of these
     * (case-insensitive, trimmed) we route to the deterministic service
     * path instead of the LLM.
     */
    private static final Set<String> QUICK_INTENTS = Set.of("loan", "account", "interest");

    private final AiService aiService;
    private final SafetyService safetyService;
    private final BankInfoService bankInfoService;
    private final LoanService loanService;
    private final LoanSchemeRepository loanSchemeRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final BankingProperties properties;

    public ChatController(AiService aiService,
                          SafetyService safetyService,
                          BankInfoService bankInfoService,
                          LoanService loanService,
                          LoanSchemeRepository loanSchemeRepository,
                          DocumentChunkRepository documentChunkRepository,
                          BankingProperties properties) {
        this.aiService = aiService;
        this.safetyService = safetyService;
        this.bankInfoService = bankInfoService;
        this.loanService = loanService;
        this.loanSchemeRepository = loanSchemeRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.properties = properties;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Chat
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Text-only banking Q&amp;A. Accepts a JSON body
     * {@code {"message": "..."}}. Returns a structured
     * {@link ChatResponse}.
     */
    @PostMapping(value = "/chat", consumes = "application/json")
    public ResponseEntity<ChatResponse> chatJson(@RequestBody ChatRequest body,
                                                 HttpServletRequest req) {
        String message = normalize(body == null ? null : body.getMessage());
        requireMessage(message);

        ChatResponse response = route(message, req);
        log.info("chat requestId={} intent={} answerLen={}",
                req.getAttribute(RequestIdFilter.ATTR), response.getIntent(),
                response.getAnswer() == null ? 0 : response.getAnswer().length());
        return ResponseEntity.ok(response);
    }

    /**
     * Multipart form variant of {@link #chatJson}. Used when a file
     * accompanies the question; the file is ingested and the question is
     * answered with the file's chunks in context.
     */
    @PostMapping(value = "/chat/upload", consumes = "multipart/form-data")
    public ResponseEntity<ChatResponse> chatUpload(
            @RequestPart(value = "message", required = false) String message,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest req) {

        String normalized = normalize(message);
        requireMessage(normalized);

        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException(
                    "Multipart upload requires a non-empty 'file' part");
        }
        validateFile(file);

        AgentResult result = aiService.processFile(normalized, file);
        ChatResponse response = new ChatResponse(
                result.finalAnswer(),
                "FILE_QA",
                Collections.singletonList(file.getOriginalFilename()),
                (String) req.getAttribute(RequestIdFilter.ATTR),
                result.error(),
                toStepViews(result.steps()));
        log.info("chat/upload requestId={} steps={} hasError={}",
                req.getAttribute(RequestIdFilter.ATTR),
                result.steps().size(), result.hasError());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Documents
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Upload-only endpoint. Extracts text, chunks it, persists the chunks,
     * returns a {@link DocumentSummary}. Does NOT call the LLM.
     */
    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public ResponseEntity<DocumentSummary> uploadDocument(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest req) {

        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("'file' part is required");
        }
        validateFile(file);

        List<DocumentChunk> saved = aiService.ingestFile(file);
        DocumentSummary summary = new DocumentSummary(
                file.getOriginalFilename(),
                file.getContentType(),
                saved.size());
        log.info("document upload requestId={} source={} chunks={}",
                req.getAttribute(RequestIdFilter.ATTR),
                summary.getSource(), summary.getChunkCount());
        return ResponseEntity.ok(summary);
    }

    /**
     * List documents that have been ingested, grouped by source filename.
     * Order is most-recently-uploaded first (by highest chunk id).
     */
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentSummary>> listDocuments() {
        List<DocumentSummary> grouped = documentChunkRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        DocumentChunk::getSource,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new DocumentSummary(
                                        list.get(0).getSource(),
                                        list.get(0).getType(),
                                        list.size()))))
                .values().stream()
                .sorted((a, b) -> Long.compare(b.getChunkCount(), a.getChunkCount()))
                .toList();
        return ResponseEntity.ok(grouped);
    }

    /**
     * Delete every chunk that came from the given source filename.
     * URL-decoded because filenames commonly contain spaces and dots.
     */
    @DeleteMapping("/documents/{source}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String source) {
        String decoded = URLDecoder.decode(source, StandardCharsets.UTF_8);
        long before = documentChunkRepository.countBySource(decoded);
        if (before == 0) {
            throw new ResourceNotFoundException(
                    "No document chunks found for source: " + decoded);
        }
        List<DocumentChunk> chunks = documentChunkRepository.findBySource(decoded);
        documentChunkRepository.deleteAll(chunks);
        log.info("Deleted {} chunks for source={}", chunks.size(), decoded);
        return ResponseEntity.ok(Map.of(
                "source", decoded,
                "deletedChunks", chunks.size()));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Loans (static catalogue)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Expose the static {@code loan_scheme} catalogue. Powers the sidebar
     * dropdown on the frontend.
     */
    @GetMapping("/loans")
    public ResponseEntity<List<LoanSummary>> listLoans() {
        List<LoanSummary> summaries = loanSchemeRepository.findAll().stream()
                .map(ChatController::toSummary)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/loans/{id}")
    public ResponseEntity<LoanSummary> getLoan(@PathVariable Long id) {
        LoanScheme loan = loanSchemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan " + id + " not found"));
        return ResponseEntity.ok(toSummary(loan));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Decide whether the message is a sidebar quick-button intent and route
     * accordingly. Otherwise, hand the question to the full RAG pipeline.
     */
    private ChatResponse route(String message, HttpServletRequest req) {
        String reqId = (String) req.getAttribute(RequestIdFilter.ATTR);

        if (safetyService.isSensitive(message)) {
            return new ChatResponse(
                    "Sorry, I cannot access or provide personal banking information.",
                    "BLOCKED", Collections.emptyList(), reqId);
        }

        String trimmed = message.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (QUICK_INTENTS.contains(lower)) {
            String intent;
            String answer;
            switch (lower) {
                case "loan" -> {
                    intent = "LOAN_INFO";
                    answer = loanService.getLoanSummary();
                }
                case "account" -> {
                    intent = "ACCOUNT_OPEN";
                    answer = bankInfoService.getInfo("ACCOUNT_OPEN");
                }
                case "interest" -> {
                    intent = "INTEREST_RATE";
                    answer = bankInfoService.getInfo("INTEREST_RATE");
                }
                default -> {
                    intent = "UNKNOWN";
                    answer = aiService.generateResponse(trimmed).finalAnswer();
                }
            }
            return new ChatResponse(answer, intent, Collections.emptyList(), reqId);
        }

        // Loan-name shortcut: if the message exactly matches a known loan,
        // return its deterministic details without an LLM round-trip.
        if (loanService.getAllLoans().stream().anyMatch(
                l -> l.getName().equalsIgnoreCase(trimmed))) {
            String details = loanService.getLoanDetails(trimmed);
            return new ChatResponse(details, "LOAN_DETAILS",
                    Collections.emptyList(), reqId);
        }

        AgentResult result = aiService.generateResponse(trimmed);
        log.info("chat requestId={} intent=RAG steps={} hasError={}",
                reqId, result.steps().size(), result.hasError());
        return new ChatResponse(
                result.finalAnswer(),
                "RAG",
                Collections.emptyList(),
                reqId,
                result.error(),
                toStepViews(result.steps()));
    }

    private void validateFile(MultipartFile file) {
        long maxSize = properties.getMaxFileSize();
        if (file.getSize() > maxSize) {
            throw new FileTooLargeException(file.getSize(), maxSize);
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new UnsupportedFileTypeException("unknown");
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        boolean ok = properties.getAllowedMimeTypes().stream()
                .anyMatch(allowed -> lower.contains(allowed.toLowerCase(Locale.ROOT)));
        if (!ok) {
            throw new UnsupportedFileTypeException(contentType);
        }
    }

    private static String normalize(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("\\s+", " ").trim();
    }

    private static void requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new InvalidRequestException("'message' must not be blank");
        }
    }

    private static List<ChatResponse.AgentStepView> toStepViews(List<AgentStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return Collections.emptyList();
        }
        return steps.stream()
                .map(s -> new ChatResponse.AgentStepView(
                        s.iteration(),
                        s.thinking(),
                        toInvocationViews(s.toolInvocations())))
                .toList();
    }

    private static List<ChatResponse.ToolInvocationView> toInvocationViews(List<ToolInvocation> invocations) {
        if (invocations == null || invocations.isEmpty()) {
            return Collections.emptyList();
        }
        return invocations.stream()
                .map(i -> new ChatResponse.ToolInvocationView(
                        i.name(), i.arguments(), i.result()))
                .toList();
    }

    private static LoanSummary toSummary(LoanScheme l) {
        LoanSummary s = new LoanSummary();
        s.setId(l.getId());
        s.setName(l.getName());
        s.setMinAmount(l.getMinAmount());
        s.setMaxAmount(l.getMaxAmount());
        s.setInterestRate(l.getInterestRate());
        s.setTargetGroup(l.getTargetGroup());
        s.setDescription(l.getDescription());
        return s;
    }
}
