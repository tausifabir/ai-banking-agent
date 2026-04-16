package com.aibankingagent.ai_banking_agent.controller;

import com.aibankingagent.ai_banking_agent.service.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ChatController {

    private final AiService aiService;
    private final SafetyService safetyService;
    private final BankInfoService bankInfoService;
    private final LoanService loanService;


    /* Constructor Injection. */
    public ChatController(AiService aiService,
                          SafetyService safetyService,
                          BankInfoService bankInfoService,
                          LoanService loanService) {
        this.aiService = aiService;
        this.safetyService = safetyService;
        this.bankInfoService = bankInfoService;
        this.loanService = loanService;
    }

    @PostMapping("/api/chat")
    public String chat(        @RequestPart(value = "message", required = false) String message,
                               @RequestPart(value = "file", required = false) MultipartFile file) {

        // 🔐 Step 1: Safety Check
        if (safetyService.isSensitive(message)) {
            return "Sorry, I cannot access or provide personal banking information.";
        }

        // 📄 If file uploaded
        if (file != null && !file.isEmpty()) {
            return aiService.processFile(message, file);
        }
        // 🧠 Step 2: Intent Detection
        return aiService.generateResponse(message);


//        // 🏦 Loan flow
//        if (intent.equals("LOAN_INFO")) {
//            return loanService.getLoanSummary();
//        }
//
//        // 🔎 If user selects loan name
//        if (message.toLowerCase().contains("loan")) {
//            return loanService.getLoanDetails(message);
//        }
//
//
//        // 🏦 Step 3: Get Response
//        return bankInfoService.getInfo(intent);
    }

}
