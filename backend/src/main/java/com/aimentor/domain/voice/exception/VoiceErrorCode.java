package com.aimentor.domain.voice.exception;

import org.springframework.http.HttpStatus;

public enum VoiceErrorCode {
    VOICE_STT_EMPTY_AUDIO(HttpStatus.BAD_REQUEST, "VOICE_STT_EMPTY_AUDIO", "음성 파일이 비어 있습니다."),
    VOICE_STT_EMPTY_TRANSCRIPT(HttpStatus.BAD_GATEWAY, "VOICE_STT_EMPTY_TRANSCRIPT", "음성 인식 결과가 비어 있습니다."),
    VOICE_STT_PROVIDER_FAILED(HttpStatus.BAD_GATEWAY, "VOICE_STT_PROVIDER_FAILED", "음성 인식 제공자 호출에 실패했습니다."),
    VOICE_TTS_EMPTY_TEXT(HttpStatus.BAD_REQUEST, "VOICE_TTS_EMPTY_TEXT", "음성으로 변환할 텍스트가 비어 있습니다."),
    VOICE_TTS_PROVIDER_FAILED(HttpStatus.BAD_GATEWAY, "VOICE_TTS_PROVIDER_FAILED", "음성 생성에 실패했습니다."),
    VOICE_TTS_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "VOICE_TTS_INVALID_RESPONSE", "음성 생성 응답 형식이 올바르지 않습니다."),
    VOICE_TTS_AUDIO_URL_MISSING(HttpStatus.BAD_GATEWAY, "VOICE_TTS_AUDIO_URL_MISSING", "재생 가능한 음성 URL이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String userMessage;

    VoiceErrorCode(HttpStatus httpStatus, String code, String userMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.userMessage = userMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String userMessage() {
        return userMessage;
    }
}

