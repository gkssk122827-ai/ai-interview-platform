package com.aimentor.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secretKey,
        long accessTokenExpirationSeconds,
        long refreshTokenExpirationSeconds
) {
}
