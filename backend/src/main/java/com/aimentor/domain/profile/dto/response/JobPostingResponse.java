package com.aimentor.domain.profile.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobPostingResponse(
        Long id,
        Long userId,
        String companyName,
        String positionTitle,
        String description,
        String fileUrl,
        String jobUrl,
        String siteName,
        String sourceStatus,
        LocalDate deadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
