package com.aimentor.domain.report.dto.response;

import java.time.LocalDateTime;

public record AdminRecentUserResponse(
        Long id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
) {
}
