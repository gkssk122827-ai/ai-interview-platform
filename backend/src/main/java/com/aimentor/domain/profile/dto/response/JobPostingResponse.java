package com.aimentor.domain.profile.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Returns job-posting data owned by the authenticated user.
 */
public record JobPostingResponse(
        Long id,
        Long userId,
        String companyName,
        String positionTitle,
        String description,
        String fileUrl,
        String jobUrl,
        LocalDate deadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
