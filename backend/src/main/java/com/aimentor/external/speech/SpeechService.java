package com.aimentor.external.speech;

/**
 * Defines the speech-to-text operation used by backend services.
 */
public interface SpeechService {

    String speechToText(String audioFile);
}
