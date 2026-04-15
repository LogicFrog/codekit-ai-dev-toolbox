package org.itfjnu.codekit.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI module configuration class (Volcengine Ark · Doubao Special)
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    /**
     * AI service provider
     */
    private String provider = "mock";

    /**
     * API key
     */
    private String apiKey = "";

    /**
     * Model name
     */
    private String model = "doubao-seed-2-0-pro-260215";

    /**
     * API base URL
     */
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";

    /**
     * Request timeout (milliseconds)
     * Default 30 seconds
     */
    private int timeout = 30000;

    /**
     * Maximum number of tokens
     */
    private int maxTokens = 2048;

    /**
     * Temperature (0-2)
     */
    private Double temperature = 1.0;

    /**
     * Check if the configuration is valid
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
