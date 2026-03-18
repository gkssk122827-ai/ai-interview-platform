package com.aimentor.domain.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Validates the answer payload for an interview question.
 */
public record SaveInterviewAnswerRequest(
        @NotNull(message = "Question ID is required.")
        Long questionId,

        @NotBlank(message = "Answer text is required.")
        @Size(max = 5000, message = "Answer text must be 5000 characters or fewer.")
        String answerText,

        @Size(max = 500, message = "Audio URL must be 500 characters or fewer.")
        String audioUrl
) {
}
