package com.aimentor.external.speech;

import com.aimentor.domain.voice.exception.VoiceException;
import com.aimentor.external.ai.AiServerProperties;
import com.aimentor.external.speech.dto.SpeechToTextRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubSpeechIntegrationServiceTest {

    @Test
    void speechToTextShouldReturnVoiceSttProviderFailedWhenDataUrlIsInvalid() {
        SpeechIntegrationProperties properties = new SpeechIntegrationProperties("python", null, null, null, 200, 200);
        AiServerProperties aiServerProperties = new AiServerProperties("http://127.0.0.1:9999");
        StubSpeechIntegrationService service = new StubSpeechIntegrationService(properties, aiServerProperties);

        assertThatThrownBy(() -> service.speechToText(new SpeechToTextRequest("invalid-data-url", "ko-KR")))
                .isInstanceOf(VoiceException.class)
                .satisfies(ex -> {
                    VoiceException voiceException = (VoiceException) ex;
                    assertThat(voiceException.getErrorCode()).isEqualTo("VOICE_STT_PROVIDER_FAILED");
                    assertThat(voiceException.getDetails()).isEqualTo("INVALID_AUDIO_DATA_URL");
                });
    }
}
