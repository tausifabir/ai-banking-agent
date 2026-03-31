package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private final LlmClient  llmClient;
    private final PromptBuilder  promptBuilder;
    private final ContextBuilder  contextBuilder;
    private final RetrieverService  retrieverService;
    private final SignalExtractorService  signalExtractorService;
    private final ResponseValidatorService  responseValidatorService;

    public AiServiceImpl(LlmClient llmClient,
                         PromptBuilder promptBuilder,
                         ContextBuilder contextBuilder,
                         RetrieverService retrieverService,
                         SignalExtractorService signalExtractorService,
                         ResponseValidatorService responseValidatorService) {
        this.llmClient = llmClient;
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

    // 🔁 Fallback Response
    private String fallbackResponse() {
        return "Service is temporarily unavailable. Please try again later.";
    }

}
