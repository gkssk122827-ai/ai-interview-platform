package com.aimentor.common.security;

import com.aimentor.domain.user.entity.Role;

public record AuthenticatedUser(
        Long userId,
        String email,
        Role role
) {
}
