package com.aimentor.domain.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Validates unified application-document create and update requests.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApplicationDocumentUpsertRequest {

    @NotBlank(message = "Document title is required.")
    @Size(max = 100, message = "Document title must be 100 characters or fewer.")
    private String title;

    @Size(max = 5000, message = "Resume text must be 5000 characters or fewer.")
    private String resumeText;

    @Size(max = 5000, message = "Cover letter text must be 5000 characters or fewer.")
    private String coverLetterText;
}
