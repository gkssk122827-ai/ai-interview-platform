package com.aimentor.domain.profile.dto.response;

import java.time.LocalDateTime;

/**
 * Returns resume data owned by the authenticated user.
 */
public record ResumeResponse(
        Long id,
        Long userId,
        String title,
        String content,
        String fileUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
