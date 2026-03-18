package com.aimentor.domain.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Validates job-posting create and update requests.
 */
public record JobPostingUpsertRequest(
        @NotBlank(message = "Company name is required.")
        @Size(max = 100, message = "Company name must be 100 characters or fewer.")
        String companyName,

        @NotBlank(message = "Position title is required.")
        @Size(max = 100, message = "Position title must be 100 characters or fewer.")
        String positionTitle,

        @NotBlank(message = "Job posting description is required.")
        @Size(max = 5000, message = "Job posting description must be 5000 characters or fewer.")
        String description,

        @Size(max = 500, message = "Job posting file URL must be 500 characters or fewer.")
        String fileUrl,

        @Size(max = 300, message = "Job posting URL must be 300 characters or fewer.")
        String jobUrl,

        LocalDate deadline
) {
}
