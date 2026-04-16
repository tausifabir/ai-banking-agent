package com.aibankingagent.ai_banking_agent.repository;

import com.aibankingagent.ai_banking_agent.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
}
