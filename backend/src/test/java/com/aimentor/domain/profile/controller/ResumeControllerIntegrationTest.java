package com.aimentor.domain.profile.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret-key=test-secret-key-test-secret-key-test-secret-key",
        "jwt.access-token-expiration-seconds=1800",
        "jwt.refresh-token-expiration-seconds=1209600"
})
@AutoConfigureMockMvc
class ResumeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // The in-memory database is recreated per test context with create-drop.
    }

    @Test
    void resumeCrudShouldBeOwnedByLoggedInUser() throws Exception {
        String userAccessToken = signupAndGetAccessToken("owner@example.com");
        String otherUserAccessToken = signupAndGetAccessToken("other@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/profiles/resumes")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Backend Resume",
                                  "content": "Spring Boot and JPA experience",
                                  "fileUrl": "https://example.com/resume.pdf"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Backend Resume"))
                .andExpect(jsonPath("$.data.fileUrl").value("https://example.com/resume.pdf"))
                .andReturn();

        Long resumeId = readId(createResult);

        mockMvc.perform(get("/api/v1/profiles/resumes")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/profiles/resumes")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken))
                        .param("keyword", "Backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Backend Resume"));

        mockMvc.perform(put("/api/v1/profiles/resumes/{resumeId}", resumeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Resume",
                                  "content": "Updated content",
                                  "fileUrl": "https://example.com/resume-v2.pdf"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Resume"))
                .andExpect(jsonPath("$.data.fileUrl").value("https://example.com/resume-v2.pdf"));

        mockMvc.perform(get("/api/v1/profiles/resumes/{resumeId}", resumeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUserAccessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESUME_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/profiles/resumes/{resumeId}", resumeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/profiles/resumes")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void resumeEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/resumes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Resume User",
                                  "email": "%s",
                                  "phone": "010-2222-2222",
                                  "password": "password1"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = response.path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }

    private Long readId(MvcResult mvcResult) throws Exception {
        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
