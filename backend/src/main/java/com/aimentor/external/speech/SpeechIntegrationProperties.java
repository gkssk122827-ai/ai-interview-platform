package com.aimentor.external.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.speech")
public record SpeechIntegrationProperties(
        String provider,
        String apiKey,
        String baseUrl,
        String voice
) {
}
