package com.aimentor.domain.profile.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

/**
 * Validates resume create and update requests.
 */
public record ResumeUpsertRequest(
        @NotBlank(message = "Resume title is required.")
        @Size(max = 100, message = "Resume title must be 100 characters or fewer.")
        String title,

        @NotBlank(message = "Resume content is required.")
        @Size(max = 5000, message = "Resume content must be 5000 characters or fewer.")
        String content,

        @Size(max = 500, message = "Resume file URL must be 500 characters or fewer.")
        String fileUrl
) {
}
