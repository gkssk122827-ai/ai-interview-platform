package com.aimentor.domain.user.controller;

import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
        "jwt.refresh-token-expiration-seconds=604800"
})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerShouldCreateUserAndEncryptPassword() throws Exception {
        String requestBody = """
                {
                  "name": "Hong Gil Dong",
                  "email": "user@example.com",
                  "phone": "010-1234-5678",
                  "password": "password1"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Hong Gil Dong"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        User savedUser = userRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(savedUser.getName()).isEqualTo("Hong Gil Dong");
        assertThat(savedUser.getPhone()).isEqualTo("010-1234-5678");
        assertThat(savedUser.getPassword()).isNotEqualTo("password1");
        assertThat(passwordEncoder.matches("password1", savedUser.getPassword())).isTrue();
        assertThat(savedUser.getRefreshToken()).isNotBlank();
    }

    @Test
    void loginShouldReturnTokensForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Login User",
                                  "email": "login@example.com",
                                  "phone": "010-0000-0001",
                                  "password": "password1"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Login User"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-0001"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void refreshShouldRotateRefreshToken() throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Refresh User",
                                  "email": "refresh@example.com",
                                  "phone": "010-0000-0002",
                                  "password": "password1"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode signupResponse = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String refreshToken = signupResponse.path("data").path("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshResponse = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = refreshResponse.path("data").path("refreshToken").asText();

        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);
    }

    @Test
    void meShouldReturnAuthenticatedUserInfo() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Profile User",
                                  "email": "me@example.com",
                                  "phone": "010-0000-0003",
                                  "password": "password1"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = response.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Profile User"))
                .andExpect(jsonPath("$.data.email").value("me@example.com"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-0003"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void logoutShouldRequireAuthenticationAndClearRefreshToken() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Logout User",
                                  "email": "logout@example.com",
                                  "phone": "010-0000-0004",
                                  "password": "password1"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = response.path("data").path("accessToken").asText();
        String refreshToken = response.path("data").path("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User savedUser = userRepository.findByEmail("logout@example.com").orElseThrow();
        assertThat(savedUser.getRefreshToken()).isNull();
    }
}
