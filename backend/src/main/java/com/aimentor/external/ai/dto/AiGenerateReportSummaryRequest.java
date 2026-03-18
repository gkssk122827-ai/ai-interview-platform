package com.aimentor.external.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiGenerateReportSummaryRequest(
        @NotBlank(message = "Session title is required.")
        @Size(max = 100, message = "Session title must be 100 characters or less.")
        String sessionTitle,

        @Size(max = 100, message = "Position title must be 100 characters or less.")
        String positionTitle,

        List<AiAnalyzeAnswerFeedbackResponse> answerFeedback
) {
}
