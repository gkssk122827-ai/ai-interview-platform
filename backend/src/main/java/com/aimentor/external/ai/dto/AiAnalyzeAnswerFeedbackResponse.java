package com.aimentor.external.ai.dto;

public record AiAnalyzeAnswerFeedbackResponse(
        int relevanceScore,
        int logicScore,
        int specificityScore,
        int overallScore,
        String feedbackSummary,
        String providerName,
        boolean stubbed
) {
}
