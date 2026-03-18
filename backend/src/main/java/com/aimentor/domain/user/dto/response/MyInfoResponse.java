package com.aimentor.domain.user.dto.response;

import com.aimentor.domain.user.entity.Role;
import java.time.LocalDateTime;

/**
 * Returns the authenticated user's profile information.
 */
public record MyInfoResponse(
        Long userId,
        String name,
        String email,
        String phone,
        Role role,
        LocalDateTime createdAt
) {
}
