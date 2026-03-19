package com.aimentor.domain.voice.controller;

import com.aimentor.external.speech.SpeechIntegrationService;
import com.aimentor.external.speech.dto.SpeechToTextResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret-key=test-secret-key-test-secret-key-test-secret-key",
        "jwt.access-token-expiration-seconds=1800",
        "jwt.refresh-token-expiration-seconds=1209600"
})
@AutoConfigureMockMvc
class VoiceControllerSttIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SpeechIntegrationService speechIntegrationService;

    @Test
    void sttShouldReturnVoiceSttEmptyAudioWhenAudioIsEmpty() throws Exception {
        String accessToken = signupAndGetAccessToken("voice-empty-audio@example.com");
        MockMultipartFile emptyAudio = new MockMultipartFile("audio", "empty.webm", "audio/webm", new byte[0]);

        mockMvc.perform(multipart("/api/stt")
                        .file(emptyAudio)
                        .param("languageCode", "ko-KR")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_STT_EMPTY_AUDIO"));
    }

    @Test
    void sttShouldReturnVoiceSttProviderFailedWhenProviderCallFails() throws Exception {
        String accessToken = signupAndGetAccessToken("voice-provider-fail@example.com");
        MockMultipartFile audio = new MockMultipartFile("audio", "sample.webm", "audio/webm", "fake-audio".getBytes());
        when(speechIntegrationService.speechToText(any())).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(multipart("/api/stt")
                        .file(audio)
                        .param("languageCode", "ko-KR")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_STT_PROVIDER_FAILED"));
    }

    @Test
    void sttShouldReturnVoiceSttEmptyTranscriptWhenProviderReturnsBlankText() throws Exception {
        String accessToken = signupAndGetAccessToken("voice-empty-transcript@example.com");
        MockMultipartFile audio = new MockMultipartFile("audio", "sample.webm", "audio/webm", "fake-audio".getBytes());
        when(speechIntegrationService.speechToText(any()))
                .thenReturn(new SpeechToTextResponse("   ", "python", false));

        mockMvc.perform(multipart("/api/stt")
                        .file(audio)
                        .param("languageCode", "ko-KR")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_STT_EMPTY_TRANSCRIPT"));
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Voice Test User",
                                  "email": "%s",
                                  "phone": "010-9999-9999",
                                  "password": "password1"
                                }
                                """.formatted(email)
                        ))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
