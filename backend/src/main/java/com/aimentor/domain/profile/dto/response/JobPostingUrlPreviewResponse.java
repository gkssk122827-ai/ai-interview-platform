package com.aimentor.domain.profile.dto.response;

public record JobPostingUrlPreviewResponse(
        String siteName,
        String jobUrl,
        String companyName,
        String positionTitle,
        String description,
        String deadline,
        boolean extracted,
        String failureReason
) {
}
