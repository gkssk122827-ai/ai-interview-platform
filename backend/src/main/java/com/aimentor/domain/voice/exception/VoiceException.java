package com.aimentor.domain.voice.exception;

import com.aimentor.common.exception.ApiException;

public class VoiceException extends ApiException {

    private final Object details;

    public VoiceException(VoiceErrorCode errorCode) {
        this(errorCode, null);
    }

    public VoiceException(VoiceErrorCode errorCode, Object details) {
        super(errorCode.httpStatus(), errorCode.code(), errorCode.userMessage());
        this.details = details;
    }

    public Object getDetails() {
        return details;
    }
}

