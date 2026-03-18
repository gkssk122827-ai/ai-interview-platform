package com.aimentor.external.ai.dto;

/**
 * Carries feedback values returned by the AI server.
 */
public record FeedbackDto(
        Integer logicScore,
        Integer relevanceScore,
        Integer specificityScore,
        Integer overallScore,
        String weakPoints,
        String improvements,
        String recommendedAnswer
) {
}
