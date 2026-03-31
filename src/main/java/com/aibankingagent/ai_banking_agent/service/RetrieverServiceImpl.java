package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.helper.QuerySignal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RetrieverServiceImpl implements RetrieverService {

    private final LoanService loanService;

    public RetrieverServiceImpl(LoanService loanService) {
        this.loanService = loanService;
    }


    @Override
    public List<LoanScheme> retrieve(QuerySignal s, String message) {
        return loanService.getAllLoans().stream()
                .map(l -> Map.entry(l, score(l, s, message))) // ✅ pass message
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private int score(LoanScheme l, QuerySignal s, String message) {

        int score = 0;

        String name = l.getName().toLowerCase();
        String msg = message.toLowerCase();

        // 🔥 1. direct keyword match (VERY IMPORTANT)
        if (msg.contains(name)) score += 5;

        // 🔥 2. purpose match
        if (s.getPurpose() != null && name.contains(s.getPurpose())) {
            score += 3;
        }

        // 🔥 3. amount match
        if (s.getAmount() != null &&
                l.getMinAmount() != null &&
                l.getMaxAmount() != null &&
                s.getAmount() >= l.getMinAmount() &&
                s.getAmount() <= l.getMaxAmount()) {
            score += 4;
        }

        return score;
    }
}
