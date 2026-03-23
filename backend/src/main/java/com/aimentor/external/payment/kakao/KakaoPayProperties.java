package com.aimentor.external.payment.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.payment.kakao")
public record KakaoPayProperties(
        boolean enabled,
        String baseUrl,
        String adminKey,
        String cid,
        String clientBaseUrl,
        int connectTimeoutMillis,
        int readTimeoutMillis
) {
}
