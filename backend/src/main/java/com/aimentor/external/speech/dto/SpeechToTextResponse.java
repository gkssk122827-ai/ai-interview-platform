package com.aimentor.external.speech.dto;

public record SpeechToTextResponse(
        String transcriptText,
        String providerName,
        boolean stubbed
) {
}
