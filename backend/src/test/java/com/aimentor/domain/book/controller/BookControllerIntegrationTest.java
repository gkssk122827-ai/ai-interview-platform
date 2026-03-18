package com.aimentor.domain.book.controller;

import com.aimentor.common.security.jwt.JwtTokenProvider;
import com.aimentor.domain.user.entity.Role;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

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
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void adminShouldManageBooksAndUserShouldReadBooks() throws Exception {
        String adminAccessToken = createAdminAccessToken("admin@example.com");
        String userAccessToken = signupAndGetAccessToken("user@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Clean Architecture",
                                  "author": "Robert C. Martin",
                                  "publisher": "Prentice Hall",
                                  "price": 32000,
                                  "stock": 15,
                                  "coverUrl": "https://example.com/clean-architecture.jpg",
                                  "description": "A guide to software structure and design."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Clean Architecture"))
                .andExpect(jsonPath("$.data.stock").value(15))
                .andReturn();

        Long bookId = readId(createResult);

        mockMvc.perform(get("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken))
                        .param("keyword", "Clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Clean Architecture"));

        mockMvc.perform(get("/api/books/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.author").value("Robert C. Martin"));

        mockMvc.perform(put("/api/books/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Clean Architecture 2nd Edition",
                                  "author": "Robert C. Martin",
                                  "publisher": "Prentice Hall",
                                  "price": 35000,
                                  "stock": 10,
                                  "coverUrl": "https://example.com/clean-architecture-2.jpg",
                                  "description": "Updated book description."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Clean Architecture 2nd Edition"))
                .andExpect(jsonPath("$.data.stock").value(10));

        mockMvc.perform(delete("/api/books/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void nonAdminShouldNotManageBooks() throws Exception {
        String userAccessToken = signupAndGetAccessToken("nonadmin@example.com");

        mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Domain-Driven Design",
                                  "author": "Eric Evans",
                                  "publisher": "Addison-Wesley",
                                  "price": 41000,
                                  "stock": 8,
                                  "coverUrl": "https://example.com/ddd.jpg",
                                  "description": "DDD reference book."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_REQUIRED"));
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Book User",
                                  "email": "%s",
                                  "phone": "010-4444-4444",
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

    private String createAdminAccessToken(String email) {
        User admin = userRepository.save(User.builder()
                .email(email)
                .name("Admin User")
                .phone("010-5555-5555")
                .password(passwordEncoder.encode("password1"))
                .role(Role.ADMIN)
                .build());

        admin.updateRefreshToken(
                "admin-refresh-token",
                LocalDateTime.now().plusDays(7)
        );

        return jwtTokenProvider.createAccessToken(admin.getId(), admin.getEmail(), admin.getRole());
    }

    private Long readId(MvcResult mvcResult) throws Exception {
        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
