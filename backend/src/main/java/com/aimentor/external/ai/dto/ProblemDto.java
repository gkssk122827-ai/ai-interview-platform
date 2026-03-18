package com.aimentor.external.ai.dto;

import java.util.List;

/**
 * Carries a generated learning problem and its answer metadata.
 */
public record ProblemDto(
        String type,
        String question,
        List<String> choices,
        String answer,
        String explanation
) {
}
