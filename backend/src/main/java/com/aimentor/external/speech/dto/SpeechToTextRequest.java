package com.aimentor.external.speech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpeechToTextRequest(
        @NotBlank(message = "Audio URL is required.")
        @Size(max = 500, message = "Audio URL must be 500 characters or less.")
        String audioUrl,

        @Size(max = 20, message = "Language code must be 20 characters or less.")
        String languageCode
) {
}
