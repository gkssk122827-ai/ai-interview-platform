package com.aimentor.domain.report.dto.response;

import java.time.LocalDateTime;

public record AdminRecentInterviewSessionResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String title,
        String positionTitle,
        String status,
        LocalDateTime startedAt,
        LocalDateTime createdAt
) {
}
