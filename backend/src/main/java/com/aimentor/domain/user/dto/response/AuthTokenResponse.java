package com.aimentor.domain.user.dto.response;

import com.aimentor.domain.user.entity.Role;
import com.aimentor.domain.user.entity.UserStatus;
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
        UserStatus status,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt
) {
}
