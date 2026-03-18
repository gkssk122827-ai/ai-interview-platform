package com.aimentor.domain.learning.dto.response;

import java.util.List;

public record LearningProblemResponse(
        String type,
        String question,
        List<String> choices,
        String answer,
        String explanation
) {
}
