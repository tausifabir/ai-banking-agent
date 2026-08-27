package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.DocumentChunk;
import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.exception.UnsupportedFileTypeException;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import com.aibankingagent.ai_banking_agent.repository.DocumentChunkRepository;
import com.aibankingagent.ai_banking_agent.tool.AgentResult;
import com.aibankingagent.ai_banking_agent.tool.LlmMessage;
import com.aibankingagent.ai_banking_agent.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final ToolExecutor toolExecutor;
    private final TextChunker chunker;
    private final OcrService ocrService;
    private final PdfService pdfService;
    private final PdfRetriever pdfRetriever;
    private final PromptBuilder promptBuilder;
    private final ContextBuilder contextBuilder;
    private final RetrieverService retrieverService;
    private final DocumentChunkRepository documentChunkRepository;
    private final SignalExtractorService signalExtractorService;

    public AiServiceImpl(ToolExecutor toolExecutor,
                         TextChunker chunker,
                         PdfRetriever pdfRetriever,
                         OcrService ocrService,
                         PdfService pdfService,
                         PromptBuilder promptBuilder,
                         ContextBuilder contextBuilder,
                         RetrieverService retrieverService,
                         DocumentChunkRepository documentChunkRepository,
                         SignalExtractorService signalExtractorService) {
        this.toolExecutor = toolExecutor;
        this.chunker = chunker;
        this.pdfRetriever = pdfRetriever;
        this.ocrService = ocrService;
        this.pdfService = pdfService;
        this.promptBuilder = promptBuilder;
        this.contextBuilder = contextBuilder;
        this.retrieverService = retrieverService;
        this.documentChunkRepository = documentChunkRepository;
        this.signalExtractorService = signalExtractorService;
    }


    @Override
    public AgentResult generateResponse(String message) {

        long startNs = System.nanoTime();

        message = message.replaceAll("\\s+", " ").trim();

        log.debug("generateResponse: message length={}", message.length());

        long t0 = System.nanoTime();
        QuerySignal signal = this.signalExtractorService.signalExtractor(message);
        long signalMs = elapsedMs(t0);

        t0 = System.nanoTime();
        List<LoanScheme> loans = this.retrieverService.retrieve(signal, message);
        long loanRetrieveMs = elapsedMs(t0);

        t0 = System.nanoTime();
        List<String> relevant = pdfRetriever.retrieveFromChunks(
                Collections.emptyList(), message, signal);
        String chunkContext = contextBuilder.buildFromPdf(relevant);
        String loanContext = this.contextBuilder.buildContext(loans);
        long contextBuildMs = elapsedMs(t0);

        StringBuilder context = new StringBuilder();
        if (chunkContext != null && !chunkContext.isBlank()) {
            context.append("📄 DOCUMENT DATA:\n").append(chunkContext).append("\n\n");
        }
        if (loanContext != null && !loanContext.isBlank()) {
            context.append("🏦 LOAN DATA:\n").append(loanContext);
        }

        List<LlmMessage> messages = this.promptBuilder.build(context.toString(), message);
        AgentResult result = this.toolExecutor.runLoop(messages);
        logAgentRun("generateResponse", startNs, signalMs, loanRetrieveMs,
                contextBuildMs, 0L, result);
        return result;
    }

    @Override
    public AgentResult processFile(String message, MultipartFile file) {

        long startNs = System.nanoTime();

        message = message.replaceAll("\\s+", " ").trim();

        long t0 = System.nanoTime();
        List<DocumentChunk> saved = ingestFile(file);
        long ingestMs = elapsedMs(t0);

        t0 = System.nanoTime();
        QuerySignal signal = this.signalExtractorService.signalExtractor(message);
        long signalMs = elapsedMs(t0);

        t0 = System.nanoTime();
        List<String> chunks = saved.stream().map(DocumentChunk::getContent).toList();
        List<String> relevant = pdfRetriever.retrieveFromChunks(chunks, message, signal);
        String context = contextBuilder.buildFromPdf(relevant);
        long contextBuildMs = elapsedMs(t0);

        List<LlmMessage> messages = promptBuilder.build(context, message);
        AgentResult result = this.toolExecutor.runLoop(messages);
        logAgentRun("processFile", startNs, signalMs, 0L, contextBuildMs,
                ingestMs, result);
        return result;
    }

    /**
     * Emit a single structured log line summarising the whole agent run.
     * Stage timings are in milliseconds; the orchestrator-side stages
     * (signal / retrieve / context / ingest) are measured here, the
     * per-iteration LLM + tool timings are logged inside {@link ToolExecutor}.
     */
    private void logAgentRun(String flow,
                             long startNs,
                             long signalMs,
                             long loanRetrieveMs,
                             long contextBuildMs,
                             long ingestMs,
                             AgentResult result) {
        long totalMs = elapsedMs(startNs);
        log.info("{} done: totalMs={} signalMs={} retrieveMs={} contextMs={} "
                        + "ingestMs={} steps={} toolCalls={} hasError={} error={}",
                flow,
                totalMs,
                signalMs,
                loanRetrieveMs,
                contextBuildMs,
                ingestMs,
                result.steps().size(),
                result.steps().stream()
                        .mapToInt(s -> s.toolInvocations() == null
                                ? 0 : s.toolInvocations().size())
                        .sum(),
                result.hasError(),
                result.error());
    }

    private static long elapsedMs(long startNs) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
    }

    @Override
    public List<DocumentChunk> ingestFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Collections.emptyList();
        }

        String contentType = file.getContentType();
        String extractedText;

        if (contentType != null && contentType.contains("pdf")) {
            extractedText = pdfService.extract(file);
        } else if (contentType != null && contentType.contains("image")) {
            extractedText = ocrService.extract(file);
        } else {
            throw new UnsupportedFileTypeException(
                    contentType == null ? "unknown" : contentType);
        }

        if (extractedText == null || extractedText.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks = chunker.chunk(extractedText);
        List<DocumentChunk> saved = new ArrayList<>(chunks.size());
        for (String c : chunks) {
            DocumentChunk dc = new DocumentChunk();
            dc.setContent(c);
            dc.setSource(file.getOriginalFilename());
            dc.setType(contentType);
            saved.add(documentChunkRepository.save(dc));
        }
        log.info("Ingested file {} -> {} chunks", file.getOriginalFilename(), saved.size());
        return saved;
    }
}
