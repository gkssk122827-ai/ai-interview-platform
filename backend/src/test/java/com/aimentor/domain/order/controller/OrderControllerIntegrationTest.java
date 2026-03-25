package com.aimentor.domain.order.controller;

import com.aimentor.common.security.jwt.JwtTokenProvider;
import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import com.aimentor.domain.cart.entity.CartItem;
import com.aimentor.domain.cart.repository.CartItemRepository;
import com.aimentor.domain.user.entity.Role;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import com.aimentor.external.payment.kakao.KakaoPayService;
import com.aimentor.external.payment.kakao.dto.KakaoPayApproveResult;
import com.aimentor.external.payment.kakao.dto.KakaoPayReadyResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

    @MockitoBean
    private KakaoPayService kakaoPayService;

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
                .andExpect(jsonPath("$.data.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.data.payments.length()").value(1))
                .andExpect(jsonPath("$.data.payments[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.payments.length()").value(1));

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
    void shouldStoreFailedPaymentAndAllowRetry() throws Exception {
        String accessToken = createUserAccessToken("failed-payment-user@example.com");
        User user = userRepository.findByEmail("failed-payment-user@example.com").orElseThrow();
        Long bookId = createBook("Effective TypeScript", 38000, 3);

        cartItemRepository.save(CartItem.builder()
                .userId(user.getId())
                .bookId(bookId)
                .quantity(1)
                .build());

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "Seoul, Teheran-ro 45"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        Long orderId = readOrderId(createResult);

        mockMvc.perform(post("/api/orders/{orderId}/pay", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CARD",
                                  "success": false,
                                  "failureReason": "테스트용 실패"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.data.paymentFailureReason").value("테스트용 실패"))
                .andExpect(jsonPath("$.data.payments.length()").value(1))
                .andExpect(jsonPath("$.data.payments[0].status").value("FAILED"));

        mockMvc.perform(post("/api/orders/{orderId}/pay", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CARD",
                                  "success": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.payments.length()").value(2))
                .andExpect(jsonPath("$.data.payments[0].status").value("SUCCESS"));
    }

    @Test
    void shouldReadyAndApproveKakaoPayment() throws Exception {
        when(kakaoPayService.ready(any())).thenReturn(new KakaoPayReadyResult("T123456789", "https://mock.kakao/redirect"));
        when(kakaoPayService.approve(any())).thenReturn(new KakaoPayApproveResult(
                "T123456789",
                "A123456789",
                "MONEY",
                LocalDateTime.of(2026, 3, 20, 13, 0)
        ));

        String accessToken = createUserAccessToken("kakao-order-user@example.com");
        User user = userRepository.findByEmail("kakao-order-user@example.com").orElseThrow();
        Long bookId = createBook("Real-World React", 28000, 4);

        cartItemRepository.save(CartItem.builder()
                .userId(user.getId())
                .bookId(bookId)
                .quantity(1)
                .build());

        Long orderId = readOrderId(mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "Seoul, Eonju-ro 12"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/orders/{orderId}/payments/kakao/ready", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "KAKAO_PAY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("KAKAO_PAY"))
                .andExpect(jsonPath("$.data.paymentMethod").value("KAKAO_PAY"))
                .andExpect(jsonPath("$.data.redirectUrl").value("https://mock.kakao/redirect"));

        mockMvc.perform(post("/api/orders/{orderId}/payments/kakao/approve", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pgToken": "pg_token_value"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paymentMethod").value("KAKAO_PAY"))
                .andExpect(jsonPath("$.data.payments[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.payments[0].provider").value("KAKAO_PAY"))
                .andExpect(jsonPath("$.data.payments[0].providerTransactionId").value("T123456789"));

        assertThat(bookRepository.findById(bookId).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void shouldStoreKakaoCancelAsFailedPayment() throws Exception {
        when(kakaoPayService.ready(any())).thenReturn(new KakaoPayReadyResult("T987654321", "https://mock.kakao/redirect"));

        String accessToken = createUserAccessToken("kakao-cancel-user@example.com");
        User user = userRepository.findByEmail("kakao-cancel-user@example.com").orElseThrow();
        Long bookId = createBook("Frontend Performance", 33000, 2);

        cartItemRepository.save(CartItem.builder()
                .userId(user.getId())
                .bookId(bookId)
                .quantity(1)
                .build());

        Long orderId = readOrderId(mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "Seoul, Seolleung-ro 10"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/orders/{orderId}/payments/kakao/ready", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "KAKAO_PAY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{orderId}/payments/kakao/cancel", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.data.paymentFailureReason").value("카카오페이 결제가 취소되었습니다."))
                .andExpect(jsonPath("$.data.payments[0].status").value("FAILED"));
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
