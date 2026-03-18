package com.aimentor.domain.interview.dto.response;

/**
 * Provides one reusable improvement hint that can later connect to learning features.
 */
public record InterviewLearningRecommendationResponse(
        String focusArea,
        String reason,
        String recommendedAction
) {
}
