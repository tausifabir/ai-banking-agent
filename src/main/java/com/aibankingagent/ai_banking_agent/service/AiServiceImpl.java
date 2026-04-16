package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.DocumentChunk;
import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import com.aibankingagent.ai_banking_agent.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    private final LlmClient  llmClient;
    private final TextChunker chunker;
    private final OcrService  ocrService;
    private final PdfService  pdfService;
    private final PdfRetriever  pdfRetriever;
    private final PromptBuilder  promptBuilder;
    private final ContextBuilder  contextBuilder;
    private final RetrieverService  retrieverService;
    private final DocumentChunkRepository documentChunkRepository;
    private final SignalExtractorService  signalExtractorService;
    private final ResponseValidatorServiceImpl responseValidatorService;

    public AiServiceImpl(LlmClient llmClient,
                         TextChunker chunker,
                         PdfRetriever pdfRetriever,
                         OcrService ocrService,
                         PdfService pdfService,
                         PromptBuilder promptBuilder,
                         ContextBuilder contextBuilder,
                         RetrieverService retrieverService, DocumentChunkRepository documentChunkRepository,
                         SignalExtractorService signalExtractorService,
                         ResponseValidatorServiceImpl responseValidatorService) {
        this.llmClient = llmClient;
        this.chunker = chunker;
        this.pdfRetriever = pdfRetriever;
        this.ocrService = ocrService;
        this.pdfService = pdfService;
        this.promptBuilder = promptBuilder;
        this.contextBuilder = contextBuilder;
        this.retrieverService = retrieverService;
        this.documentChunkRepository = documentChunkRepository;
        this.signalExtractorService = signalExtractorService;
        this.responseValidatorService = responseValidatorService;
    }


    @Override
    public String generateResponse(String message) {

        message = message.replaceAll("\\s+", " ").trim();

        // 1. Extract signal
        QuerySignal signal = this.signalExtractorService.signalExtractor(message);

//        if (!signal.isLoan()) {
//            return "I can help with loan related queries.";
//        }

        // 2. Retrieve (RAG)
        List<LoanScheme> loans = this.retrieverService.retrieve(signal, message);

        // 2. chunk
        List<String> chunks = new ArrayList<>();

        // 3. Retrieve (signal + keyword)
        List<String> relevant = pdfRetriever.retrieveFromChunks(chunks, message, signal);

        // 4. Build context from chunk
        String chunkContext = contextBuilder.buildFromPdf(relevant);

        // 5. Build context from loan info
        String loanContext = this.contextBuilder.buildContext(loans);


        StringBuilder context = new StringBuilder();

        if (chunkContext != null && !chunkContext.isBlank()) {
            context.append("📄 DOCUMENT DATA:\n")
                    .append(chunkContext)
                    .append("\n\n");
        }

        if (loanContext != null && !loanContext.isBlank()) {
            context.append("🏦 LOAN DATA:\n")
                    .append(loanContext);
        }


        // 6. Build prompt
        String prompt = this.promptBuilder.build(String.valueOf(context), message);

        // 7. Call LLM
        try {
            String rawResponse = this.llmClient.call(prompt);
            // 8. Validate
            return this.responseValidatorService.validate(rawResponse);
        } catch (Exception e) {
            return fallbackResponse();
        }

    }

    @Override
    public String processFile(String message, MultipartFile file) {

        message = message.replaceAll("\\s+", " ").trim();

        this.validateFile(file);

        String contentType = file.getContentType();

        String extractedText;

        if (contentType != null && contentType.contains("pdf")) {
            extractedText = pdfService.extract(file);
        } else if (contentType != null && contentType.contains("image")) {
            extractedText = ocrService.extract(file);
        } else {
            return "Unsupported file type.";
        }

        // 1. Extract signal
        QuerySignal signal = this.signalExtractorService.signalExtractor(message);

        // 2. chunk
        List<String> chunks = new ArrayList<>();

        if(extractedText != null) {
            chunks = chunker.chunk(extractedText);
            // ✅ SAVE to DB
            for (String c : chunks) {
                DocumentChunk dc = new DocumentChunk();
                dc.setContent(c);
                dc.setSource(file.getOriginalFilename());
                dc.setType(contentType);
                this.documentChunkRepository.save(dc);
            }
        }


        // 3. Retrieve (signal + keyword)
        List<String> relevant = pdfRetriever.retrieveFromChunks(chunks, message, signal);

//        if (relevant.isEmpty()) {
//            return searchTool.search(message);
//        }


        // 4. Build context
        String context = contextBuilder.buildFromPdf(relevant);

        // 5. Build prompt
        String prompt = promptBuilder.build(context, message);


        // 6. Call LLM
        try {
            String rawResponse = this.llmClient.call(prompt);
            // 7. Validate
            return this.responseValidatorService.validate(rawResponse);
        } catch (Exception e) {
            return fallbackResponse();
        }
    }

    private void validateFile(MultipartFile file) {

        long maxSize = 5 * 1024 * 1024; // 5MB

        if (file.getSize() > maxSize) {
            throw new RuntimeException("File exceeds 5MB limit");
        }
    }

    // 🔁 Fallback Response
    private String fallbackResponse() {
        return "Service is temporarily unavailable. Please try again later.";
    }

}
