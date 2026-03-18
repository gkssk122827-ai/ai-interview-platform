package com.aimentor.external.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provides the Python AI server base URL used by external integrations.
 */
@ConfigurationProperties(prefix = "ai.server")
public record AiServerProperties(
        String url
) {
}
