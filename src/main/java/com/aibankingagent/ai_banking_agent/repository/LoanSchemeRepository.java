package com.aibankingagent.ai_banking_agent.repository;

import com.aibankingagent.ai_banking_agent.entity.LoanScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanSchemeRepository extends JpaRepository<LoanScheme, Long> {
}
