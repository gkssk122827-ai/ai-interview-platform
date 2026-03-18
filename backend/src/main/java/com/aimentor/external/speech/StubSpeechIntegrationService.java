package com.aimentor.external.speech;

import com.aimentor.external.speech.dto.SpeechToTextRequest;
import com.aimentor.external.speech.dto.SpeechToTextResponse;
import com.aimentor.external.speech.dto.TextToSpeechRequest;
import com.aimentor.external.speech.dto.TextToSpeechResponse;
import org.springframework.stereotype.Service;

@Service
public class StubSpeechIntegrationService implements SpeechIntegrationService {

    private final SpeechIntegrationProperties properties;

    public StubSpeechIntegrationService(SpeechIntegrationProperties properties) {
        this.properties = properties;
    }

    @Override
    public SpeechToTextResponse speechToText(SpeechToTextRequest request) {
        // Real provider integration will download/process audio and call an STT provider here.
        return new SpeechToTextResponse(
                "Stub transcript for audio: " + request.audioUrl(),
                resolveProviderName(),
                true
        );
    }

    @Override
    public TextToSpeechResponse textToSpeech(TextToSpeechRequest request) {
        // Real provider integration will synthesize speech and return provider audio metadata here.
        return new TextToSpeechResponse(
                "stub://tts/" + Integer.toHexString(request.text().hashCode()),
                resolveProviderName(),
                true
        );
    }

    private String resolveProviderName() {
        return properties.provider() == null || properties.provider().isBlank()
                ? "stub-speech"
                : properties.provider();
    }
}
