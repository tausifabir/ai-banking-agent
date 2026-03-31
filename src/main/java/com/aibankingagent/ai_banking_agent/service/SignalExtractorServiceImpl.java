package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import org.springframework.stereotype.Service;

@Service
public class SignalExtractorServiceImpl implements SignalExtractorService {
    @Override
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
}
