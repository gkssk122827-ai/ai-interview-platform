package com.aimentor.external.speech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextToSpeechRequest(
        @NotBlank(message = "Text is required.")
        @Size(max = 5000, message = "Text must be 5000 characters or less.")
        String text,

        @Size(max = 50, message = "Voice name must be 50 characters or less.")
        String voiceName,

        @Size(max = 20, message = "Language code must be 20 characters or less.")
        String languageCode
) {
}
