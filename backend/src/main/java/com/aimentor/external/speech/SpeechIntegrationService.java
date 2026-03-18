package com.aimentor.external.speech;

import com.aimentor.external.speech.dto.SpeechToTextRequest;
import com.aimentor.external.speech.dto.SpeechToTextResponse;
import com.aimentor.external.speech.dto.TextToSpeechRequest;
import com.aimentor.external.speech.dto.TextToSpeechResponse;

/**
 * Defines the backend-facing contract for speech-related integrations.
 */
public interface SpeechIntegrationService {

    SpeechToTextResponse speechToText(SpeechToTextRequest request);

    TextToSpeechResponse textToSpeech(TextToSpeechRequest request);
}
