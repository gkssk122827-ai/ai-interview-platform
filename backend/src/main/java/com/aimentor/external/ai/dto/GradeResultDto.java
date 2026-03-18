package com.aimentor.external.ai.dto;

/**
 * Carries the grading result returned by the AI server.
 */
public record GradeResultDto(
        boolean isCorrect,
        String aiFeedback
) {
}
