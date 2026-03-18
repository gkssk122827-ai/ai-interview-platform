package com.aimentor.external.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.ai")
public record AiIntegrationProperties(
        String provider,
        String apiKey,
        String baseUrl,
        String model
) {
}
