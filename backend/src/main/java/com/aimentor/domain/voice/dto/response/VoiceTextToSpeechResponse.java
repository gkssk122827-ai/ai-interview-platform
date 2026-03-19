package com.aimentor.domain.voice.dto.response;

public record VoiceTextToSpeechResponse(
        String audioUrl,
        String providerName,
        boolean stubbed
) {
}

