package com.aibankingagent.ai_banking_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiBankingAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiBankingAgentApplication.class, args);
	}

}
