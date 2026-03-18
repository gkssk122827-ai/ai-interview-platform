package com.aimentor.domain.interview.controller;

import com.aimentor.domain.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class InterviewSessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void interviewSessionLifecycleShouldWork() throws Exception {
        String accessToken = signupAndGetAccessToken("interview@example.com");
        String adminAccessToken = signupAndGetAccessToken("interview-admin@example.com", Role.ADMIN);
        Long resumeId = createProfileDocument(
                accessToken,
                "/api/v1/profiles/resumes",
                """
                        {
                          "title": "Backend Resume",
                          "content": "Built Spring Boot services and REST APIs."
                        }
                        """
        );
        Long coverLetterId = createProfileDocument(
                accessToken,
                "/api/v1/profiles/cover-letters",
                """
                        {
                          "title": "Backend Cover Letter",
                          "companyName": "AI Mentor",
                          "content": "I want to join as a backend engineer."
                        }
                        """
        );
        Long jobPostingId = createProfileDocument(
                adminAccessToken,
                "/api/v1/profiles/job-postings",
                """
                        {
                          "companyName": "AI Mentor",
                          "positionTitle": "Backend Engineer",
                          "description": "Build interview platform APIs.",
                          "fileUrl": "https://example.com/job-posting.pdf",
                          "jobUrl": "https://example.com/jobs/backend-engineer"
                        }
                        """
        );

        MvcResult startResult = mockMvc.perform(post("/api/interviews/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Backend Mock Interview",
                                  "positionTitle": "Backend Engineer",
                                  "mode": "TECHNICAL",
                                  "resumeId": %d,
                                  "coverLetterId": %d,
                                  "jobPostingId": %d,
                                  "questionCount": 2
                                }
                                """.formatted(resumeId, coverLetterId, jobPostingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONGOING"))
                .andExpect(jsonPath("$.data.mode").value("TECHNICAL"))
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andReturn();

        JsonNode startResponse = objectMapper.readTree(startResult.getResponse().getContentAsString());
        Long sessionId = startResponse.path("data").path("id").asLong();
        Long questionId = startResponse.path("data").path("questions").get(0).path("id").asLong();

        mockMvc.perform(get("/api/interviews/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/api/interviews/sessions/{sessionId}/answer", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": %d,
                                  "answerText": "I have built several Spring Boot services.",
                                  "audioUrl": ""
                                }
                                """.formatted(questionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerText").value("I have built several Spring Boot services."))
                .andExpect(jsonPath("$.data.audioUrl").isNotEmpty());

        mockMvc.perform(get("/api/interviews/sessions/{sessionId}", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].answerText").value("I have built several Spring Boot services."));

        mockMvc.perform(get("/api/interviews/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore").isNumber())
                .andExpect(jsonPath("$.data.weakPoints").isNotEmpty())
                .andExpect(jsonPath("$.data.recommendedAnswer").isNotEmpty());

        mockMvc.perform(post("/api/interviews/sessions/{sessionId}/end", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.feedback.overallScore").isNumber());
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        return signupAndGetAccessToken(email, Role.USER);
    }

    private String signupAndGetAccessToken(String email, Role role) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Interview User",
                                  "email": "%s",
                                  "phone": "010-1111-1111",
                                  "password": "password1"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        if (role == Role.ADMIN) {
            jdbcTemplate.update("UPDATE users SET role = ? WHERE email = ?", Role.ADMIN.name(), email);
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "%s",
                                      "password": "password1"
                                    }
                                    """.formatted(email)))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode response = objectMapper.readTree(loginResult.getResponse().getContentAsString());
            String accessToken = response.path("data").path("accessToken").asText();
            assertThat(accessToken).isNotBlank();
            return accessToken;
        }

        JsonNode response = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String accessToken = response.path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private Long createProfileDocument(String accessToken, String uri, String requestBody) throws Exception {
        MvcResult result = mockMvc.perform(post(uri)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }
}
