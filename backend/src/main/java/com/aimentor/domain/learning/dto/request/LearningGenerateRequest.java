package com.aimentor.domain.learning.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LearningGenerateRequest(
        @NotBlank(message = "subject is required.")
        String subject,

        @NotBlank(message = "difficulty is required.")
        @Pattern(regexp = "EASY|MEDIUM|HARD", message = "difficulty must be EASY, MEDIUM, or HARD.")
        String difficulty,

        @Min(value = 1, message = "count must be at least 1.")
        @Max(value = 10, message = "count must be at most 10.")
        int count,

        @NotBlank(message = "type is required.")
        @Pattern(regexp = "MULTIPLE|SHORT|MIX", message = "type must be MULTIPLE, SHORT, or MIX.")
        String type
) {
}
