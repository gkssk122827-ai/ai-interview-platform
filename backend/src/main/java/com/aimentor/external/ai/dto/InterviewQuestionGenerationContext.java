package com.aimentor.external.ai.dto;

import java.util.List;

/**
 * Carries structured context for generating one interview question.
 */
public record InterviewQuestionGenerationContext(
        String interviewMode,
        String positionCategory,
        String questionDifficulty,
        int questionIndex,
        int totalQuestionCount,
        String modeGuide,
        List<String> materialHighlights,
        List<String> existingQuestions
) {
}
