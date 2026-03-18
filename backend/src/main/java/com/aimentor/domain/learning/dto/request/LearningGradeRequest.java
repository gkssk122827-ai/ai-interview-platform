package com.aimentor.domain.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LearningGradeRequest(
        @NotBlank(message = "question is required.")
        String question,

        @NotBlank(message = "correctAnswer is required.")
        String correctAnswer,

        @NotBlank(message = "userAnswer is required.")
        String userAnswer,

        @Size(max = 5000, message = "explanation must be 5000 characters or fewer.")
        String explanation
) {
}
