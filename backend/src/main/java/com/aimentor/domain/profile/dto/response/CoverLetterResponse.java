package com.aimentor.domain.profile.dto.response;

import java.time.LocalDateTime;

/**
 * Returns cover-letter data owned by the authenticated user.
 */
public record CoverLetterResponse(
        Long id,
        Long userId,
        String title,
        String companyName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
