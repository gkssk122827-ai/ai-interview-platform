package com.aimentor.domain.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validates cover-letter create and update requests.
 */
public record CoverLetterUpsertRequest(
        @NotBlank(message = "Cover letter title is required.")
        @Size(max = 100, message = "Cover letter title must be 100 characters or fewer.")
        String title,

        @NotBlank(message = "Company name is required.")
        @Size(max = 100, message = "Company name must be 100 characters or fewer.")
        String companyName,

        @NotBlank(message = "Cover letter content is required.")
        @Size(max = 5000, message = "Cover letter content must be 5000 characters or fewer.")
        String content
) {
}
