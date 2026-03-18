package com.aimentor.external.speech;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Provides deterministic speech-to-text output for local development and testing.
 */
@Service
@ConditionalOnProperty(prefix = "integration.speech", name = "provider", havingValue = "mock-speech", matchIfMissing = true)
public class MockSpeechService implements SpeechService {

    @Override
    public String speechToText(String audioFile) {
        return "Mock transcript for audio file: " + audioFile;
    }
}
