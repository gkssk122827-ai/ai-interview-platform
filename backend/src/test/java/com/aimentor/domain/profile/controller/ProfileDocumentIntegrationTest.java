package com.aimentor.domain.profile.controller;

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
class ProfileDocumentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void coverLetterCrudShouldBeOwnedByLoggedInUser() throws Exception {
        String accessToken = signupAndGetAccessToken("cover@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/profiles/cover-letters")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Backend Cover Letter",
                                  "companyName": "AI Mentor",
                                  "content": "I want to contribute to backend services."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("AI Mentor"))
                .andReturn();

        Long coverLetterId = readId(createResult);

        mockMvc.perform(get("/api/v1/profiles/cover-letters/{coverLetterId}", coverLetterId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Backend Cover Letter"))
                .andExpect(jsonPath("$.data.userId").isNumber());

        mockMvc.perform(put("/api/v1/profiles/cover-letters/{coverLetterId}", coverLetterId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Cover Letter",
                                  "companyName": "AI Mentor",
                                  "content": "Updated content."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Cover Letter"));

        mockMvc.perform(delete("/api/v1/profiles/cover-letters/{coverLetterId}", coverLetterId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void jobPostingCrudShouldPersistFileUrl() throws Exception {
        String accessToken = signupAndGetAccessToken("jobposting@example.com", Role.ADMIN);

        MvcResult createResult = mockMvc.perform(post("/api/v1/profiles/job-postings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "AI Mentor",
                                  "positionTitle": "Backend Engineer",
                                  "description": "Build APIs for interview and learning services.",
                                  "fileUrl": "https://example.com/job-posting.pdf",
                                  "jobUrl": "https://example.com/jobs/backend-engineer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileUrl").value("https://example.com/job-posting.pdf"))
                .andReturn();

        Long jobPostingId = readId(createResult);

        mockMvc.perform(get("/api/v1/profiles/job-postings/{jobPostingId}", jobPostingId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("AI Mentor"))
                .andExpect(jsonPath("$.data.positionTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.data.fileUrl").value("https://example.com/job-posting.pdf"));

        mockMvc.perform(put("/api/v1/profiles/job-postings/{jobPostingId}", jobPostingId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "AI Mentor",
                                  "positionTitle": "Senior Backend Engineer",
                                  "description": "Updated description.",
                                  "fileUrl": "https://example.com/job-posting-v2.pdf",
                                  "jobUrl": "https://example.com/jobs/backend-engineer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionTitle").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.data.fileUrl").value("https://example.com/job-posting-v2.pdf"));
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        return signupAndGetAccessToken(email, Role.USER);
    }

    private String signupAndGetAccessToken(String email, Role role) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Profile User",
                                  "email": "%s",
                                  "phone": "010-3333-3333",
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

    private Long readId(MvcResult mvcResult) throws Exception {
        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
