package com.aimentor.domain.report.dto.response;

import java.time.LocalDateTime;

public record AdminRecentApplicationDocumentResponse(
        Long id,
        Long userId,
        String title,
        String userName,
        String userEmail,
        LocalDateTime createdAt
) {
}
