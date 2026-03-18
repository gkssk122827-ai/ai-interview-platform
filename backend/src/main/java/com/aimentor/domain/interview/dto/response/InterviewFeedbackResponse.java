package com.aimentor.domain.interview.dto.response;

import java.time.LocalDateTime;

/**
 * Returns feedback for an interview session.
 */
public record InterviewFeedbackResponse(
        Long id,
        Integer relevanceScore,
        Integer logicScore,
        Integer specificityScore,
        Integer overallScore,
        String weakPoints,
        String improvements,
        String recommendedAnswer,
        LocalDateTime createdAt
) {
}
