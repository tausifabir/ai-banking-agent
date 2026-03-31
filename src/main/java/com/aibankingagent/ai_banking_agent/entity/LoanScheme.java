package com.aibankingagent.ai_banking_agent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "loan_scheme")
@Getter
@Setter
public class LoanScheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double minAmount;
    private Double maxAmount;
    private String interestRate;

    @Column(length = 1000)
    private String description;

    private String targetGroup;
}
