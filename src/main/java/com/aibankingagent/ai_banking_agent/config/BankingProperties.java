package com.aibankingagent.ai_banking_agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Externalized limits for the controller layer. Bound to keys under the
 * {@code banking.upload.*} prefix in {@code application.properties}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banking.upload")
public class BankingProperties {

    /**
     * Maximum allowed upload size in bytes. Default 5 MB.
     */
    private long maxFileSize = 5L * 1024 * 1024;

    /**
     * MIME types accepted by the upload endpoints. Matched with
     * {@link String#contains} on the upload's content type, so partial
     * matches like {@code "pdf"} or {@code "image"} work.
     */
    private List<String> allowedMimeTypes = List.of("pdf", "image");
}
