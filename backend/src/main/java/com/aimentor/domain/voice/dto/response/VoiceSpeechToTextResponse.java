package com.aimentor.domain.voice.dto.response;

public record VoiceSpeechToTextResponse(
        String transcriptText,
        String providerName,
        boolean stubbed
) {
}

