package com.aimentor.external.speech;

import com.aimentor.external.ai.AiServerProperties;
import com.aimentor.external.ai.AiServiceException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls the external Python AI server for speech-to-text conversion.
 */
@Service
@ConditionalOnProperty(prefix = "integration.speech", name = "provider", havingValue = "python")
public class PythonSpeechService implements SpeechService {

    private final RestTemplate restTemplate;
    private final AiServerProperties aiServerProperties;

    public PythonSpeechService(AiServerProperties aiServerProperties) {
        this.restTemplate = new RestTemplate();
        this.aiServerProperties = aiServerProperties;
    }

    @Override
    public String speechToText(String audioFile) {
        try {
            ResponseEntity<SpeechToTextResult> response = restTemplate.exchange(
                    aiServerProperties.url() + "/speech/stt",
                    HttpMethod.POST,
                    new HttpEntity<>(new SpeechToTextRequest(audioFile)),
                    SpeechToTextResult.class
            );
            return response.getBody() == null ? null : response.getBody().text();
        } catch (RestClientException ex) {
            throw new AiServiceException("Failed to convert speech to text using Python AI server.", ex);
        }
    }

    /**
     * Maps the backend speech request to the Python AI server contract.
     */
    private record SpeechToTextRequest(
            String audioFile
    ) {
    }

    /**
     * Reads the Python AI server speech-to-text response body.
     */
    private record SpeechToTextResult(
            String text
    ) {
    }
}
