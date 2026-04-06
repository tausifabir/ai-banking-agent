package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final SignalExtractorService  signalExtractorService;
    private final ResponseValidatorServiceImpl responseValidatorService;

    public AiServiceImpl(LlmClient llmClient,
                         TextChunker chunker,
                         PdfRetriever pdfRetriever,
                         OcrService ocrService,
                         PdfService pdfService,
                         PromptBuilder promptBuilder,
                         ContextBuilder contextBuilder,
                         RetrieverService retrieverService,
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
        this.signalExtractorService = signalExtractorService;
        this.responseValidatorService = responseValidatorService;
    }


    @Override
    public String generateResponse(String message) {
        // 1. Extract signal
        QuerySignal signal = this.signalExtractorService.signalExtractor(message);

//        if (!signal.isLoan()) {
//            return "I can help with loan related queries.";
//        }

        // 2. Retrieve (RAG)
        List<LoanScheme> loans = this.retrieverService.retrieve(signal, message);

/*        if (loans.isEmpty()) {
            return "Sorry, no matching loan found.";
        }*/

        // 3. Build context
        String context = this.contextBuilder.buildContext(loans);

        // 4. Build prompt
        String prompt = this.promptBuilder.build(context, message);

        // 5. Call LLM
        try {
            String rawResponse = this.llmClient.call(prompt);
            // 6. Validate
            return this.responseValidatorService.validate(rawResponse);
        } catch (Exception e) {
            return fallbackResponse();
        }

    }

    @Override
    public String processFile(String message, MultipartFile file) {

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
        List<String> chunks = chunker.chunk(extractedText);

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
