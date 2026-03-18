package com.aimentor.domain.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Validates the input required to start an interview session.
 */
public record StartInterviewSessionRequest(
        @NotBlank(message = "Interview title is required.")
        @Size(max = 100, message = "Interview title must be 100 characters or fewer.")
        String title,

        @NotBlank(message = "Position title is required.")
        @Size(max = 100, message = "Position title must be 100 characters or fewer.")
        String positionTitle,

        Long applicationDocumentId,

        Long resumeId,

        Long coverLetterId,

        Long jobPostingId,

        @Positive(message = "Question count must be at least 1.")
        Integer questionCount
) {
}
