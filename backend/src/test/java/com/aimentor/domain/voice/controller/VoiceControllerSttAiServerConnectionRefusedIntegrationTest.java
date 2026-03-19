package com.aimentor.domain.voice.controller;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
class VoiceControllerSttAiServerConnectionRefusedIntegrationTest {

    private static final int REFUSED_PORT = findUnusedPort();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("integration.speech.provider", () -> "python");
        registry.add("integration.speech.connect-timeout-ms", () -> 200);
        registry.add("integration.speech.read-timeout-ms", () -> 200);
        registry.add("ai.server.url", () -> "http://127.0.0.1:" + REFUSED_PORT);
    }

    @Test
    void sttShouldReturnProviderFailedWhenAiServerConnectionIsRefused() throws Exception {
        String accessToken = signupAndGetAccessToken("voice-fault-connection-refused@example.com");
        MockMultipartFile audio = new MockMultipartFile("audio", "sample.webm", "audio/webm", "fake-audio".getBytes());

        mockMvc.perform(multipart("/api/stt")
                        .file(audio)
                        .param("languageCode", "ko-KR")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_STT_PROVIDER_FAILED"))
                .andExpect(jsonPath("$.error.details").value("ResourceAccessException:ConnectException"));
    }

    private static int findUnusedPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to reserve an unused port for test.", ex);
        }
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Voice Fault User",
                                  "email": "%s",
                                  "phone": "010-6666-6666",
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

