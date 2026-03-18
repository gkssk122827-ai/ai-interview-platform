package com.aimentor.domain.order.controller;

import com.aimentor.common.security.jwt.JwtTokenProvider;
import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import com.aimentor.domain.cart.entity.CartItem;
import com.aimentor.domain.cart.repository.CartItemRepository;
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
class OrderControllerIntegrationTest {

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

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void shouldCreatePendingOrderAndPayIt() throws Exception {
        String accessToken = createUserAccessToken("order-user@example.com");
        User user = userRepository.findByEmail("order-user@example.com").orElseThrow();
        Long firstBookId = createBook("Clean Code", 30000, 10);
        Long secondBookId = createBook("Refactoring", 45000, 5);

        cartItemRepository.save(CartItem.builder()
                .userId(user.getId())
                .bookId(firstBookId)
                .quantity(2)
                .build());
        cartItemRepository.save(CartItem.builder()
                .userId(user.getId())
                .bookId(secondBookId)
                .quantity(1)
                .build());

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "Seoul, Gangnam-daero 123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalPrice").value(105000))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn();

        Long orderId = readOrderId(createResult);

        assertThat(bookRepository.findById(firstBookId).orElseThrow().getStock()).isEqualTo(10);
        assertThat(bookRepository.findById(secondBookId).orElseThrow().getStock()).isEqualTo(5);
        assertThat(cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).isEmpty();

        mockMvc.perform(post("/api/orders/{orderId}/pay", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(orderId))
                .andExpect(jsonPath("$.data[0].status").value("PAID"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("Seoul, Gangnam-daero 123"))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        assertThat(bookRepository.findById(firstBookId).orElseThrow().getStock()).isEqualTo(8);
        assertThat(bookRepository.findById(secondBookId).orElseThrow().getStock()).isEqualTo(4);
    }

    @Test
    void shouldReturnOutOfStockWhenPayingOrder() throws Exception {
        String accessToken = createUserAccessToken("stock-order-user@example.com");
        User user = userRepository.findByEmail("stock-order-user@example.com").orElseThrow();
        Long bookId = createBook("Domain-Driven Design", 50000, 1);

        cartItemRepository.save(CartItem.builder()
                .userId(user.getId())
                .bookId(bookId)
                .quantity(2)
                .build());

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "Busan, Haeundae 45"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        Long orderId = readOrderId(createResult);

        mockMvc.perform(post("/api/orders/{orderId}/pay", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OUT_OF_STOCK"));
    }

    @Test
    void unauthenticatedUserShouldNotAccessOrders() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private Long createBook(String title, int price, int stock) {
        return bookRepository.save(Book.builder()
                .title(title)
                .author("Author")
                .publisher("Publisher")
                .price(BigDecimal.valueOf(price))
                .stock(stock)
                .coverUrl("https://example.com/book.jpg")
                .description("Description")
                .build()).getId();
    }

    private String createUserAccessToken(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .name("Order User")
                .phone("010-8888-8888")
                .password(passwordEncoder.encode("password1"))
                .role(Role.USER)
                .build());

        user.updateRefreshToken(
                "order-refresh-token",
                LocalDateTime.now().plusDays(7)
        );

        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
    }

    private Long readOrderId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
