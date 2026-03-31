package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    private final WebClient webClient;
    private final LoanService loanService;

    public AiServiceImpl(LoanService loanService) {
        this.loanService = loanService;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    @Override
    public String detectIntent(String message) {

        List<LoanScheme> loans = loanService.getAllLoans();
        
        String loanDataAsText = this.buildContext(loans);

        String prompt = buildPrompt(loanDataAsText, message);

        Map<String, Object> request = Map.of(
                "model", "llama3",
                "prompt", prompt,
                "stream", false
        );

        Map response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response.get("response").toString().trim();

    }

    @Override
    public String generateResponse(String message) {

        //List<LoanScheme> loans = loanService.getAllLoans();

        //String loanDataAsText = this.buildContext(loans);

        QuerySignal signal = signalExtractor(message);
        String context = this.build(signal);


        String prompt = buildPrompt(context, message);

        return callOllama(prompt);
//        try {
//            return callOllama(prompt);
//        } catch (Exception e) {
//            return fallbackResponse();
//        }
    }

    // 🔁 Fallback Response
    private String fallbackResponse() {
        return "Service is temporarily unavailable. Please try again later.";
    }

    private String buildContext(List<LoanScheme> loans) {

        return loans.stream()
                .map(loan -> String.format(
                        "Name: %s | Min: %s | Max: %s | Interest: %s | Description: %s",
                        loan.getName(),
                        valueOrNA(loan.getMinAmount()),
                        valueOrNA(loan.getMaxAmount()),
                        valueOrNA(loan.getInterestRate()),
                        valueOrNA(loan.getDescription())
                ))
                .collect(Collectors.joining("\n"));
    }

    // 🧩 Utility
    private String valueOrNA(Object value) {
        return value != null ? value.toString() : "N/A";
    }


    private String buildPrompt(String loanDataAsText, String message) {
        return """
                            You are a banking assistant.
                            Answer the user's question ONLY using the provided data.
                            Do NOT make up information.
                            If answer is not found, say: "Sorry, I don't have that information."
                            ---------------------
                            Available Loan Data:
                            %s
                            ---------------------
                            User Question:
                            %s
                            Give a clear and helpful answer.
                        """.formatted(loanDataAsText, message);
    }

    private String callOllama(String prompt) {

        Map<String, Object> request = Map.of(
                "model", "llama3",
                "prompt", prompt,
                "stream", false
        );

        Map response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response.get("response").toString().trim();
    }


    public QuerySignal signalExtractor(String message) {

        String msg = message.toLowerCase();

        QuerySignal s = new QuerySignal();

        // detect signals
        s.setLoan(contains(msg, "loan", "borrow", "finance", "sme"));
        s.setAccount(contains(msg, "account", "open", "create"));
        s.setInterest(contains(msg, "interest", "rate"));

        // detect purpose
        if (msg.contains("home")) s.setPurpose("home");
        if (msg.contains("business") || msg.contains("sme")) s.setPurpose("sme");

        // detect amount (simple)
        s.setAmount(extractAmount(msg));

        return s;
    }

    private boolean contains(String msg, String... keys) {
        for (String k : keys) {
            if (msg.contains(k)) return true;
        }
        return false;
    }

    private Double extractAmount(String msg) {
        try {
            String num = msg.replaceAll("[^0-9]", "");
            return num.isEmpty() ? null : Double.parseDouble(num);
        } catch (Exception e) {
            return null;
        }
    }


    public String build(QuerySignal s) {

        List<String> parts = new ArrayList<>();

        if (s.isLoan()) {
            parts.add(buildLoanContext(s));
        }

        if (s.isAccount()) {
            parts.add(accountContext());
        }

        if (s.isInterest()) {
            parts.add(interestContext());
        }

        if (parts.isEmpty()) {
            parts.add("General banking assistant.");
        }

        return String.join("\n", parts);
    }

    // 🔥 Loan Context (filtered + short)
    private String buildLoanContext(QuerySignal s) {
        return loanService.getAllLoans().stream()
                .map(l -> Map.entry(l, score(l, s)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(3)
                .map(e -> formatLoan(e.getKey()))
                .collect(Collectors.joining("\n"));
    }

    private int score(LoanScheme l, QuerySignal s) {

        int score = 0;
        String name = l.getName().toLowerCase();

        if (s.getPurpose() != null && name.contains(s.getPurpose())) score += 3;

        if (s.getAmount() != null &&
                l.getMinAmount() != null &&
                l.getMaxAmount() != null &&
                s.getAmount() >= l.getMinAmount() &&
                s.getAmount() <= l.getMaxAmount()) score += 4;

        return score;
    }

    private String formatLoan(LoanScheme l) {
        return String.format("%s | %s-%s BDT | %s",
                l.getName(),
                val(l.getMinAmount()),
                val(l.getMaxAmount()),
                shortText(l.getDescription()));
    }

    private String accountContext() {
        return "Account Opening: NID required | Initial deposit | Fill application form";
    }

    private String interestContext() {
        return "Interest: SME ~13.75% (variable) | Others depend on scheme";
    }

    private String val(Double v) {
        return v != null ? String.valueOf(v.longValue()) : "-";
    }

    private String shortText(String d) {
        if (d == null) return "";
        return d.length() > 40 ? d.substring(0, 40) + "..." : d;
    }

}
