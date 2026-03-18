package com.aimentor.external.ai.dto;

public record AiGenerateReportSummaryResponse(
        String reportSummary,
        String providerName,
        boolean stubbed
) {
}
