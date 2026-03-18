package com.aimentor.domain.interview.dto.response;

import java.time.LocalDateTime;

/**
 * Returns an answer saved for a specific interview question.
 */
public record InterviewAnswerResponse(
        Long id,
        String answerText,
        String audioUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
