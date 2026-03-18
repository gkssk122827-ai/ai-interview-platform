package com.aimentor.external.ai.dto;

import java.util.List;

public record AiGenerateInterviewQuestionsResponse(
        List<AiQuestionItem> questions,
        String providerName,
        boolean stubbed
) {
}
