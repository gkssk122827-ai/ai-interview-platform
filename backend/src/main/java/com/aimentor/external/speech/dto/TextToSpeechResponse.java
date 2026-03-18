package com.aimentor.external.speech.dto;

public record TextToSpeechResponse(
        String audioUrl,
        String providerName,
        boolean stubbed
) {
}
