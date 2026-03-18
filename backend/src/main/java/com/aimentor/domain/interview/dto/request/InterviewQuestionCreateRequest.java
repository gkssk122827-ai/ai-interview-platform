package com.aimentor.domain.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InterviewQuestionCreateRequest(
        @NotBlank(message = "Question text is required.")
        @Size(max = 1000, message = "Question text must be 1000 characters or less.")
        String questionText
) {
}
