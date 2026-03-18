package com.aimentor.domain.profile.dto.response;

import java.time.LocalDateTime;

/**
 * Returns a unified application document owned by the authenticated user.
 */
public record ApplicationDocumentResponse(
        Long id,
        Long userId,
        String title,
        String resumeText,
        String coverLetterText,
        String originalFileName,
        String storedFilePath,
        String fileUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
