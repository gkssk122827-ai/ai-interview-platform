package com.aimentor.common.security.jwt;

import com.aimentor.domain.user.entity.Role;
import java.time.Instant;

public record JwtTokenClaims(
        Long userId,
        String email,
        Role role,
        JwtTokenType tokenType,
        Instant expiresAt
) {
}
