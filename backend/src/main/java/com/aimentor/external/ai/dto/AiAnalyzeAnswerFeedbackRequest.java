package com.aimentor.external.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAnalyzeAnswerFeedbackRequest(
        @NotBlank(message = "Question text is required.")
        @Size(max = 1000, message = "Question text must be 1000 characters or less.")
        String questionText,

        @NotBlank(message = "Answer text is required.")
        @Size(max = 5000, message = "Answer text must be 5000 characters or less.")
        String answerText,

        @Size(max = 2000, message = "Job description must be 2000 characters or less.")
        String jobDescription
) {
}
