package com.aimentor.domain.voice.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
class VoiceControllerSttAiServerFaultIntegrationTest {

    private enum Scenario {
        INTERNAL_ERROR,
        EMPTY_TRANSCRIPT,
        TIMEOUT
    }

    private static HttpServer testAiServer;
    private static volatile Scenario scenario = Scenario.INTERNAL_ERROR;
    private static String aiServerUrl;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() throws IOException {
        testAiServer = HttpServer.create(new InetSocketAddress(0), 0);
        testAiServer.createContext("/stt", VoiceControllerSttAiServerFaultIntegrationTest::handleSttRequest);
        testAiServer.start();
        aiServerUrl = "http://127.0.0.1:" + testAiServer.getAddress().getPort();
    }

    @AfterAll
    static void tearDown() {
        if (testAiServer != null) {
            testAiServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("integration.speech.provider", () -> "python");
        registry.add("integration.speech.connect-timeout-ms", () -> 200);
        registry.add("integration.speech.read-timeout-ms", () -> 200);
        registry.add("ai.server.url", () -> aiServerUrl);
    }

    @Test
    void sttShouldReturnProviderFailedWhenAiServerReturns500() throws Exception {
        scenario = Scenario.INTERNAL_ERROR;
        String accessToken = signupAndGetAccessToken("voice-fault-500@example.com");
        MockMultipartFile audio = new MockMultipartFile("audio", "sample.webm", "audio/webm", "fake-audio".getBytes());

        mockMvc.perform(multipart("/api/stt")
                        .file(audio)
                        .param("languageCode", "ko-KR")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_STT_PROVIDER_FAILED"))
                .andExpect(jsonPath("$.error.details").value("InternalServerError"));
    }

    @Test
    void sttShouldReturnEmptyTranscriptWhenAiServerReturnsBlankText() throws Exception {
        scenario = Scenario.EMPTY_TRANSCRIPT;
        String accessToken = signupAndGetAccessToken("voice-fault-empty-transcript@example.com");
        MockMultipartFile audio = new MockMultipartFile("audio", "sample.webm", "audio/webm", "fake-audio".getBytes());

        mockMvc.perform(multipart("/api/stt")
                        .file(audio)
                        .param("languageCode", "ko-KR")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_STT_EMPTY_TRANSCRIPT"));
    }

    @Test
    void sttShouldReturnProviderFailedWhenAiServerTimesOut() throws Exception {
        scenario = Scenario.TIMEOUT;
        String accessToken = signupAndGetAccessToken("voice-fault-timeout@example.com");
        MockMultipartFile audio = new MockMultipartFile("audio", "sample.webm", "audio/webm", "fake-audio".getBytes());

        mockMvc.perform(multipart("/api/stt")
                        .file(audio)
                        .param("languageCode", "ko-KR")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_STT_PROVIDER_FAILED"))
                .andExpect(jsonPath("$.error.details").value("ResourceAccessException:SocketTimeoutException"));
    }

    private static void handleSttRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        exchange.getRequestBody().readAllBytes();

        switch (scenario) {
            case INTERNAL_ERROR -> writeResponse(exchange, 500, "{\"message\":\"internal\"}");
            case EMPTY_TRANSCRIPT -> writeResponse(exchange, 200, "{\"text\":\"   \"}");
            case TIMEOUT -> {
                try {
                    Thread.sleep(1200L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                writeResponse(exchange, 200, "{\"text\":\"late-response\"}");
            }
            default -> writeResponse(exchange, 500, "{\"message\":\"unexpected\"}");
        }
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        } finally {
            exchange.close();
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
                                  "phone": "010-7777-7777",
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
