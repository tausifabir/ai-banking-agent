package com.aibankingagent.ai_banking_agent.repository;

import com.aibankingagent.ai_banking_agent.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * All chunks that came from a given source filename. Used by the
     * document-delete endpoint and by the document-list endpoint to
     * group counts per source.
     */
    List<DocumentChunk> findBySource(String source);

    /**
     * Chunk count for a given source filename. Cheaper than loading the
     * whole list when only the count is needed (list endpoint).
     */
    long countBySource(String source);
}
