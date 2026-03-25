package com.aimentor.domain.interview.controller;

import com.aimentor.domain.user.entity.Role;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        Long applicationDocumentId = createProfileDocument(
                accessToken,
                "/api/v1/profile-documents",
                """
                        {
                          "title": "Backend Application",
                          "resumeText": "Built Spring Boot services and REST APIs for high-traffic systems.",
                          "coverLetterText": "I improved API latency and collaborated with frontend teams."
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
                                  "applicationDocumentId": %d,
                                  "jobPostingId": %d,
                                  "questionCount": 2
                                }
                                """.formatted(applicationDocumentId, jobPostingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONGOING"))
                .andExpect(jsonPath("$.data.mode").value("TECHNICAL"))
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andExpect(jsonPath("$.data.questions[0].questionText").value(org.hamcrest.Matchers.containsString("Spring Boot")))
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

    @Test
    void frontendInterviewShouldUsePdfContentAndAvoidBackendQuestions() throws Exception {
        String accessToken = signupAndGetAccessToken("frontend-interview@example.com");
        String adminAccessToken = signupAndGetAccessToken("frontend-interview-admin@example.com", Role.ADMIN);
        Long applicationDocumentId = createApplicationDocumentWithPdf(
                accessToken,
                "Frontend Portfolio",
                """
                        Built React and TypeScript screens for a commerce dashboard.
                        Improved rendering performance and accessibility for large tables.
                        Managed client state and API integration with Zustand.
                        """
        );
        Long jobPostingId = createProfileDocument(
                adminAccessToken,
                "/api/v1/profiles/job-postings",
                """
                        {
                          "companyName": "AI Mentor",
                          "positionTitle": "Frontend Engineer",
                          "description": "Build React UI, improve accessibility, optimize rendering performance, and collaborate with design.",
                          "fileUrl": "https://example.com/job-posting-frontend.pdf",
                          "jobUrl": "https://example.com/jobs/frontend-engineer"
                        }
                        """
        );

        MvcResult startResult = mockMvc.perform(post("/api/interviews/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Frontend Mock Interview",
                                  "positionTitle": "프론트엔드 개발자",
                                  "mode": "TECHNICAL",
                                  "applicationDocumentId": %d,
                                  "jobPostingId": %d,
                                  "questionCount": 3
                                }
                                """.formatted(applicationDocumentId, jobPostingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions.length()").value(3))
                .andReturn();

        JsonNode response = objectMapper.readTree(startResult.getResponse().getContentAsString());
        List<String> questions = List.of(
                response.path("data").path("questions").get(0).path("questionText").asText(),
                response.path("data").path("questions").get(1).path("questionText").asText(),
                response.path("data").path("questions").get(2).path("questionText").asText()
        );

        assertThat(questions).allMatch(question -> !question.contains("Spring Boot"));
        assertThat(questions).allMatch(question -> !question.contains("JPA"));
        assertThat(questions).anyMatch(question -> question.contains("React") || question.contains("TypeScript"));
        assertThat(questions).anyMatch(question -> question.contains("렌더링") || question.contains("상태") || question.contains("API"));
        assertThat(questions.stream().distinct().count()).isEqualTo(3);
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

    private Long createApplicationDocumentWithPdf(String accessToken, String title, String pdfText) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "frontend-portfolio.pdf",
                "application/pdf",
                createPdfBytes(pdfText)
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/profile-documents")
                        .file(file)
                        .param("title", title)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private byte[] createPdfBytes(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.setLeading(16);
                contentStream.newLineAtOffset(72, 720);
                for (String line : text.strip().split("\\R")) {
                    contentStream.showText(line.trim());
                    contentStream.newLine();
                }
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
