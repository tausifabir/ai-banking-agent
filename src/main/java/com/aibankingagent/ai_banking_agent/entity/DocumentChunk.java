package com.aibankingagent.ai_banking_agent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source; // file name

    @Column(length = 2000)
    private String content;

    private String type; // PDF / IMAGE
}
