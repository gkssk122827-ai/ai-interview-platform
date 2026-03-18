package com.aimentor.domain.learning.dto.response;

import java.util.List;

public record LearningGenerateResponse(
        List<LearningProblemResponse> problems
) {
}
