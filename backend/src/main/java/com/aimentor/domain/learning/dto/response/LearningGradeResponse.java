package com.aimentor.domain.learning.dto.response;

public record LearningGradeResponse(
        boolean isCorrect,
        String aiFeedback
) {
}
