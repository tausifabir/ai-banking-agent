package com.aibankingagent.ai_banking_agent.service;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import com.aibankingagent.ai_banking_agent.repository.LoanSchemeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanSchemeRepository loanSchemeRepository;

    public LoanServiceImpl(LoanSchemeRepository loanSchemeRepository) {
        this.loanSchemeRepository = loanSchemeRepository;
    }

    @Override
    public String getLoanDetails(String name) {
        List<LoanScheme> loans = loanSchemeRepository.findAll();

        for (LoanScheme loan : loans) {
            if (loan.getName().toLowerCase().contains(name.toLowerCase())) {

                return loan.getName() + ":\n"
                        + "Min Amount: " + loan.getMinAmount() + "\n"
                        + "Max Amount: " + loan.getMaxAmount() + "\n"
                        + "Interest: " + loan.getInterestRate() + "\n"
                        + loan.getDescription();
            }
        }

        return "Sorry, loan not found.";
    }

    @Override
    public List<LoanScheme> getAllLoans() {
        return loanSchemeRepository.findAll();
    }

    @Override
    public String getLoanSummary() {
        List<LoanScheme> loans = loanSchemeRepository.findAll();

        StringBuilder response = new StringBuilder("We offer the following loans:\n");

        for (LoanScheme loan : loans) {
            response.append("- ").append(loan.getName()).append("\n");
        }

        response.append("\nWhich loan are you interested in?");

        return response.toString();
    }
}
