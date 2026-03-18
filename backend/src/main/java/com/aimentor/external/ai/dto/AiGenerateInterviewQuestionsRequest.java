package com.aimentor.external.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiGenerateInterviewQuestionsRequest(
        @NotBlank(message = "Position title is required.")
        @Size(max = 100, message = "Position title must be 100 characters or less.")
        String positionTitle,

        @Size(max = 2000, message = "Resume summary must be 2000 characters or less.")
        String resumeSummary,

        @Size(max = 2000, message = "Job description must be 2000 characters or less.")
        String jobDescription,

        @Min(value = 1, message = "Question count must be at least 1.")
        int questionCount
) {
}
