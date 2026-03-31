package com.aibankingagent.ai_banking_agent.helper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySignal {
    boolean loan;
    boolean account;
    boolean interest;
    String purpose;   // home, sme
    Double amount;
}
