package com.aimentor.domain.cart.controller;

import com.aimentor.common.security.jwt.JwtTokenProvider;
import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import com.aimentor.domain.user.entity.Role;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class CartItemControllerIntegrationTest {

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

    @Autowired
    private BookRepository bookRepository;

    @Test
    void authenticatedUserShouldManageOwnCart() throws Exception {
        String accessToken = signupAndGetAccessToken("cart-user@example.com");
        Long bookId = createBook("Effective Java", 45000, 10);

        mockMvc.perform(post("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": %d,
                                  "quantity": 2
                                }
                                """.formatted(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(bookId))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.linePrice").value(90000));

        mockMvc.perform(post("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": %d,
                                  "quantity": 1
                                }
                                """.formatted(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(3));

        mockMvc.perform(get("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(3))
                .andExpect(jsonPath("$.data.totalPrice").value(135000));

        mockMvc.perform(put("/api/cart/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(5))
                .andExpect(jsonPath("$.data.linePrice").value(225000));

        mockMvc.perform(delete("/api/cart/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.totalPrice").value(0));
    }

    @Test
    void unauthenticatedUserShouldNotAccessCart() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectQuantityExceedingStock() throws Exception {
        String accessToken = createUserAccessToken("stock-user@example.com");
        Long bookId = createBook("Spring in Action", 38000, 2);

        mockMvc.perform(post("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": %d,
                                  "quantity": 3
                                }
                                """.formatted(bookId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CART_QUANTITY_EXCEEDS_STOCK"));
    }

    private Long createBook(String title, int price, int stock) {
        return bookRepository.save(Book.builder()
                .title(title)
                .author("Joshua Bloch")
                .publisher("Tech Press")
                .price(BigDecimal.valueOf(price))
                .stock(stock)
                .coverUrl("https://example.com/book.jpg")
                .description("Book description")
                .build()).getId();
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cart User",
                                  "email": "%s",
                                  "phone": "010-6666-6666",
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

    private String createUserAccessToken(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .name("Cart User")
                .phone("010-7777-7777")
                .password(passwordEncoder.encode("password1"))
                .role(Role.USER)
                .build());

        user.updateRefreshToken(
                "user-refresh-token",
                LocalDateTime.now().plusDays(7)
        );

        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
