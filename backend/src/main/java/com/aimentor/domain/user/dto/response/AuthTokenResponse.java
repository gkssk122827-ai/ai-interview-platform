package com.aimentor.domain.user.dto.response;

import com.aimentor.domain.user.entity.Role;
import java.time.LocalDateTime;

/**
 * Returns issued JWT tokens together with basic user information.
 */
public record AuthTokenResponse(
        Long userId,
        String name,
        String email,
        String phone,
        Role role,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt
) {
}
