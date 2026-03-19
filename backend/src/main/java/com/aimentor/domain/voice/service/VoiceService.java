package com.aimentor.domain.voice.service;

import com.aimentor.domain.voice.dto.request.VoiceTextToSpeechRequest;
import com.aimentor.domain.voice.dto.response.VoiceSpeechToTextResponse;
import com.aimentor.domain.voice.dto.response.VoiceTextToSpeechResponse;
import com.aimentor.domain.voice.exception.VoiceErrorCode;
import com.aimentor.domain.voice.exception.VoiceException;
import com.aimentor.external.speech.SpeechIntegrationService;
import com.aimentor.external.speech.dto.SpeechToTextRequest;
import com.aimentor.external.speech.dto.SpeechToTextResponse;
import com.aimentor.external.speech.dto.TextToSpeechRequest;
import com.aimentor.external.speech.dto.TextToSpeechResponse;
import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoiceService {

    private final SpeechIntegrationService speechIntegrationService;

    public VoiceService(SpeechIntegrationService speechIntegrationService) {
        this.speechIntegrationService = speechIntegrationService;
    }

    public VoiceSpeechToTextResponse speechToText(MultipartFile audio, String languageCode) {
        if (audio == null || audio.isEmpty()) {
            throw new VoiceException(VoiceErrorCode.VOICE_STT_EMPTY_AUDIO);
        }
        try {
            String audioDataUrl = toDataUrl(audio);
            SpeechToTextResponse response = speechIntegrationService.speechToText(
                    new SpeechToTextRequest(audioDataUrl, languageCode)
            );
            if (response == null || !StringUtils.hasText(response.transcriptText())) {
                throw new VoiceException(VoiceErrorCode.VOICE_STT_EMPTY_TRANSCRIPT);
            }
            return new VoiceSpeechToTextResponse(
                    response.transcriptText(),
                    response.providerName(),
                    response.stubbed()
            );
        } catch (VoiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new VoiceException(
                    VoiceErrorCode.VOICE_STT_PROVIDER_FAILED,
                    ex.getClass().getSimpleName()
            );
        }
    }

    public VoiceTextToSpeechResponse textToSpeech(VoiceTextToSpeechRequest request) {
        if (request == null || !StringUtils.hasText(request.text())) {
            throw new VoiceException(VoiceErrorCode.VOICE_TTS_EMPTY_TEXT);
        }
        try {
            TextToSpeechResponse response = speechIntegrationService.textToSpeech(
                    new TextToSpeechRequest(request.text(), request.voiceName(), request.languageCode())
            );
            if (response == null) {
                throw new VoiceException(VoiceErrorCode.VOICE_TTS_INVALID_RESPONSE);
            }
            if (!StringUtils.hasText(response.audioUrl()) || !response.audioUrl().startsWith("data:audio/")) {
                throw new VoiceException(VoiceErrorCode.VOICE_TTS_AUDIO_URL_MISSING);
            }
            return new VoiceTextToSpeechResponse(
                    response.audioUrl(),
                    response.providerName(),
                    response.stubbed()
            );
        } catch (VoiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new VoiceException(
                    VoiceErrorCode.VOICE_TTS_PROVIDER_FAILED,
                    ex.getClass().getSimpleName()
            );
        }
    }

    private String toDataUrl(MultipartFile audio) {
        try {
            String contentType = audio.getContentType() == null || audio.getContentType().isBlank()
                    ? "application/octet-stream"
                    : audio.getContentType();
            String encoded = Base64.getEncoder().encodeToString(audio.getBytes());
            return "data:" + contentType + ";base64," + encoded;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded audio file.", ex);
        }
    }
}
