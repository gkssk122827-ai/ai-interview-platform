package com.aimentor.domain.order.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import com.aimentor.domain.cart.entity.CartItem;
import com.aimentor.domain.cart.repository.CartItemRepository;
import com.aimentor.domain.order.dto.request.OrderCreateRequest;
import com.aimentor.domain.order.dto.request.OrderPayRequest;
import com.aimentor.domain.order.dto.request.OrderPaymentApproveRequest;
import com.aimentor.domain.order.dto.request.OrderPaymentReadyRequest;
import com.aimentor.domain.order.dto.response.OrderItemResponse;
import com.aimentor.domain.order.dto.response.OrderPaymentReadyResponse;
import com.aimentor.domain.order.dto.response.OrderResponse;
import com.aimentor.domain.order.dto.response.PaymentTransactionResponse;
import com.aimentor.domain.order.entity.Order;
import com.aimentor.domain.order.entity.OrderItem;
import com.aimentor.domain.order.entity.OrderStatus;
import com.aimentor.domain.order.entity.PaymentTransaction;
import com.aimentor.domain.order.entity.PaymentTransactionStatus;
import com.aimentor.domain.order.repository.OrderRepository;
import com.aimentor.domain.order.repository.PaymentTransactionRepository;
import com.aimentor.external.payment.kakao.KakaoPayProperties;
import com.aimentor.external.payment.kakao.KakaoPayService;
import com.aimentor.external.payment.kakao.dto.KakaoPayApproveCommand;
import com.aimentor.external.payment.kakao.dto.KakaoPayApproveResult;
import com.aimentor.external.payment.kakao.dto.KakaoPayReadyCommand;
import com.aimentor.external.payment.kakao.dto.KakaoPayReadyResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Manages order creation and payment lifecycle for the authenticated user.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String MOCK_PAYMENT_PROVIDER = "MOCK_PAY";
    private static final String KAKAO_PAY_PROVIDER = "KAKAO_PAY";
    private static final String KAKAO_PAY_METHOD = "KAKAO_PAY";

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final KakaoPayService kakaoPayService;
    private final KakaoPayProperties kakaoPayProperties;

    public OrderService(
            OrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            BookRepository bookRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            KakaoPayService kakaoPayService,
            KakaoPayProperties kakaoPayProperties
    ) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.kakaoPayService = kakaoPayService;
        this.kakaoPayProperties = kakaoPayProperties;
    }

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CART_EMPTY", "Cart is empty.");
        }

        BigDecimal totalPrice = calculateTotalPrice(cartItems);

        Order order = Order.builder()
                .userId(userId)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .address(request.address())
                .orderedAt(LocalDateTime.now())
                .build();

        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            order.addOrderItem(OrderItem.builder()
                    .bookId(book.getId())
                    .quantity(cartItem.getQuantity())
                    .price(book.getPrice())
                    .build());
        }

        Order savedOrder = orderRepository.save(order);
        List<OrderItemResponse> createdItems = buildCreatedItemResponses(savedOrder, cartItems);
        cartItemRepository.deleteAllInBatch(cartItems);
        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus(),
                savedOrder.getAddress(),
                savedOrder.getPaymentMethod(),
                savedOrder.getPaymentFailureReason(),
                savedOrder.getOrderedAt(),
                savedOrder.getPaidAt(),
                createdItems,
                List.of()
        );
    }

    @Transactional
    public OrderResponse payOrder(Long userId, Long orderId, OrderPayRequest request) {
        Order order = getOrderEntity(userId, orderId);
        validatePayable(order);
        String paymentMethod = resolveMockPaymentMethod(request);
        boolean shouldSucceed = request == null || request.success() == null || request.success();
        LocalDateTime requestedAt = LocalDateTime.now();

        if (!shouldSucceed) {
            completeFailedPayment(
                    order,
                    PaymentTransaction.builder()
                            .provider(MOCK_PAYMENT_PROVIDER)
                            .paymentMethod(paymentMethod)
                            .transactionKey(buildTransactionKey(order.getId(), MOCK_PAYMENT_PROVIDER, requestedAt))
                            .amount(order.getTotalPrice())
                            .status(PaymentTransactionStatus.FAILED)
                            .failureReason(resolveMockFailureReason(request))
                            .requestedAt(requestedAt)
                            .approvedAt(null)
                            .build(),
                    paymentMethod,
                    resolveMockFailureReason(request)
            );
            return toResponse(order);
        }

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .provider(MOCK_PAYMENT_PROVIDER)
                .paymentMethod(paymentMethod)
                .transactionKey(buildTransactionKey(order.getId(), MOCK_PAYMENT_PROVIDER, requestedAt))
                .amount(order.getTotalPrice())
                .status(PaymentTransactionStatus.SUCCESS)
                .failureReason(null)
                .requestedAt(requestedAt)
                .approvedAt(LocalDateTime.now())
                .build();
        completeSuccessfulPayment(order, paymentTransaction, paymentMethod, paymentTransaction.getApprovedAt());
        return toResponse(order);
    }

    @Transactional
    public OrderPaymentReadyResponse readyKakaoPayment(Long userId, Long orderId, OrderPaymentReadyRequest request) {
        Order order = getOrderEntity(userId, orderId);
        validatePayable(order);
        failLatestReadyTransactionIfPresent(order, "새 카카오페이 결제 시도가 시작되었습니다.");

        LocalDateTime requestedAt = LocalDateTime.now();
        String partnerOrderId = buildPartnerOrderId(order.getId(), requestedAt);
        String partnerUserId = buildPartnerUserId(userId);
        String transactionKey = buildTransactionKey(order.getId(), KAKAO_PAY_PROVIDER, requestedAt);

        KakaoPayReadyResult readyResult = kakaoPayService.ready(new KakaoPayReadyCommand(
                order.getId(),
                userId,
                partnerOrderId,
                partnerUserId,
                buildItemName(order),
                calculateTotalQuantity(order),
                order.getTotalPrice(),
                buildClientCallbackUrl(order.getId(), "success"),
                buildClientCallbackUrl(order.getId(), "cancel"),
                buildClientCallbackUrl(order.getId(), "fail")
        ));

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .provider(KAKAO_PAY_PROVIDER)
                .paymentMethod(resolveKakaoPaymentMethod(request))
                .transactionKey(transactionKey)
                .providerTransactionId(readyResult.tid())
                .partnerOrderId(partnerOrderId)
                .partnerUserId(partnerUserId)
                .amount(order.getTotalPrice())
                .status(PaymentTransactionStatus.READY)
                .failureReason(null)
                .requestedAt(requestedAt)
                .approvedAt(null)
                .build();
        order.preparePayment(KAKAO_PAY_METHOD);
        order.addPaymentTransaction(paymentTransaction);

        return new OrderPaymentReadyResponse(
                order.getId(),
                KAKAO_PAY_PROVIDER,
                KAKAO_PAY_METHOD,
                transactionKey,
                readyResult.redirectUrl()
        );
    }

    @Transactional
    public OrderResponse approveKakaoPayment(Long userId, Long orderId, OrderPaymentApproveRequest request) {
        if (!StringUtils.hasText(request.pgToken())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PG_TOKEN_REQUIRED", "pg_token 값이 필요합니다.");
        }

        Order order = getOrderEntity(userId, orderId);
        if (order.getStatus() == OrderStatus.PAID) {
            return toResponse(order);
        }

        PaymentTransaction paymentTransaction = getLatestReadyTransaction(order);
        if (paymentTransaction == null) {
            return toResponse(order);
        }

        try {
            KakaoPayApproveResult approveResult = kakaoPayService.approve(new KakaoPayApproveCommand(
                    order.getId(),
                    userId,
                    paymentTransaction.getProviderTransactionId(),
                    paymentTransaction.getPartnerOrderId(),
                    paymentTransaction.getPartnerUserId(),
                    request.pgToken().trim()
            ));

            completeSuccessfulPayment(order, paymentTransaction, KAKAO_PAY_METHOD, approveResult.approvedAt());
            paymentTransaction.markSuccess(approveResult.tid(), approveResult.approvedAt());
            return toResponse(order);
        } catch (ApiException ex) {
            completeFailedPayment(order, paymentTransaction, KAKAO_PAY_METHOD, ex.getMessage());
            return toResponse(order);
        }
    }

    @Transactional
    public OrderResponse cancelKakaoPayment(Long userId, Long orderId) {
        return completeKakaoFailure(userId, orderId, "카카오페이 결제가 취소되었습니다.");
    }

    @Transactional
    public OrderResponse failKakaoPayment(Long userId, Long orderId) {
        return completeKakaoFailure(userId, orderId, "카카오페이 결제에 실패했습니다.");
    }

    public List<OrderResponse> getOrders(Long userId) {
        return orderRepository.findByUserIdOrderByOrderedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        return toResponse(getOrderEntity(userId, orderId));
    }

    private OrderResponse completeKakaoFailure(Long userId, Long orderId, String failureReason) {
        Order order = getOrderEntity(userId, orderId);
        if (order.getStatus() == OrderStatus.PAID) {
            return toResponse(order);
        }

        PaymentTransaction paymentTransaction = paymentTransactionRepository
                .findFirstByOrderIdAndStatusOrderByRequestedAtDesc(order.getId(), PaymentTransactionStatus.READY)
                .orElse(null);

        if (paymentTransaction == null) {
            if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
                return toResponse(order);
            }

            completeFailedPayment(
                    order,
                    PaymentTransaction.builder()
                            .provider(KAKAO_PAY_PROVIDER)
                            .paymentMethod(KAKAO_PAY_METHOD)
                            .transactionKey(buildTransactionKey(order.getId(), KAKAO_PAY_PROVIDER, LocalDateTime.now()))
                            .amount(order.getTotalPrice())
                            .status(PaymentTransactionStatus.FAILED)
                            .failureReason(failureReason)
                            .requestedAt(LocalDateTime.now())
                            .approvedAt(null)
                            .build(),
                    KAKAO_PAY_METHOD,
                    failureReason
            );
            return toResponse(order);
        }

        completeFailedPayment(order, paymentTransaction, KAKAO_PAY_METHOD, failureReason);
        return toResponse(order);
    }

    private Order getOrderEntity(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found."));
    }

    private void validatePayable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_NOT_PAYABLE", "Order cannot be paid in its current status.");
        }
    }

    private String resolveMockPaymentMethod(OrderPayRequest request) {
        if (request == null || !StringUtils.hasText(request.paymentMethod())) {
            return "CARD";
        }
        return request.paymentMethod().trim().toUpperCase();
    }

    private String resolveKakaoPaymentMethod(OrderPaymentReadyRequest request) {
        if (request == null || !StringUtils.hasText(request.paymentMethod())) {
            return KAKAO_PAY_METHOD;
        }
        return request.paymentMethod().trim().toUpperCase();
    }

    private String resolveMockFailureReason(OrderPayRequest request) {
        if (request == null || !StringUtils.hasText(request.failureReason())) {
            return "모의 결제가 실패로 설정되었습니다.";
        }
        return request.failureReason().trim();
    }

    private String buildTransactionKey(Long orderId, String provider, LocalDateTime timestamp) {
        return provider + "-" + orderId + "-" + timestamp.toString().replace(":", "").replace(".", "");
    }

    private String buildPartnerOrderId(Long orderId, LocalDateTime requestedAt) {
        return "ORDER-" + orderId + "-" + requestedAt.toString().replace(":", "").replace(".", "");
    }

    private String buildPartnerUserId(Long userId) {
        return "USER-" + userId;
    }

    private String buildItemName(Order order) {
        if (order.getOrderItems().isEmpty()) {
            return "주문 상품";
        }

        OrderItem firstItem = order.getOrderItems().get(0);
        String firstTitle = firstItem.getBook().getTitle();
        if (order.getOrderItems().size() == 1) {
            return firstTitle;
        }
        return firstTitle + " 외 " + (order.getOrderItems().size() - 1) + "건";
    }

    private int calculateTotalQuantity(Order order) {
        return order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    private String buildClientCallbackUrl(Long orderId, String callback) {
        String clientBaseUrl = StringUtils.hasText(kakaoPayProperties.clientBaseUrl())
                ? kakaoPayProperties.clientBaseUrl()
                : "http://localhost:5173";

        return UriComponentsBuilder.fromUriString(clientBaseUrl)
                .path("/payment/result")
                .queryParam("orderId", orderId)
                .queryParam("callback", callback)
                .build()
                .toUriString();
    }

    private PaymentTransaction getLatestReadyTransaction(Order order) {
        return paymentTransactionRepository.findFirstByOrderIdAndStatusOrderByRequestedAtDesc(order.getId(), PaymentTransactionStatus.READY)
                .orElseGet(() -> {
                    if (order.getStatus() == OrderStatus.PAYMENT_FAILED || order.getStatus() == OrderStatus.PAID) {
                        return null;
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_NOT_READY", "승인할 카카오페이 결제 준비 정보가 없습니다.");
                });
    }

    private void failLatestReadyTransactionIfPresent(Order order, String failureReason) {
        paymentTransactionRepository.findFirstByOrderIdAndStatusOrderByRequestedAtDesc(order.getId(), PaymentTransactionStatus.READY)
                .ifPresent(paymentTransaction -> completeFailedPayment(order, paymentTransaction, KAKAO_PAY_METHOD, failureReason));
    }

    private Book getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book not found."));
    }

    private void completeSuccessfulPayment(Order order, PaymentTransaction paymentTransaction, String paymentMethod, LocalDateTime approvedAt) {
        for (OrderItem orderItem : order.getOrderItems()) {
            Book book = getBook(orderItem.getBookId());
            if (book.getStock() < orderItem.getQuantity()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK", "Book stock is insufficient.");
            }
        }

        for (OrderItem orderItem : order.getOrderItems()) {
            Book book = getBook(orderItem.getBookId());
            book.decreaseStock(orderItem.getQuantity());
        }

        if (paymentTransaction.getOrder() == null) {
            order.addPaymentTransaction(paymentTransaction);
        }
        paymentTransaction.markSuccess(paymentTransaction.getProviderTransactionId(), approvedAt);
        order.markPaid(paymentMethod, approvedAt);
    }

    private void completeFailedPayment(Order order, PaymentTransaction paymentTransaction, String paymentMethod, String failureReason) {
        if (paymentTransaction.getOrder() == null) {
            order.addPaymentTransaction(paymentTransaction);
        }
        paymentTransaction.markFailed(failureReason);
        order.markPaymentFailed(paymentMethod, failureReason);
    }

    private BigDecimal calculateTotalPrice(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(cartItem -> cartItem.getBook().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderItemResponse> buildCreatedItemResponses(Order order, List<CartItem> cartItems) {
        List<OrderItemResponse> items = new ArrayList<>();
        for (int i = 0; i < order.getOrderItems().size(); i++) {
            OrderItem orderItem = order.getOrderItems().get(i);
            Book book = cartItems.get(i).getBook();
            items.add(new OrderItemResponse(
                    orderItem.getId(),
                    order.getId(),
                    orderItem.getBookId(),
                    orderItem.getQuantity(),
                    orderItem.getPrice(),
                    book.getTitle(),
                    book.getCoverUrl()
            ));
        }
        return items;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(orderItem -> new OrderItemResponse(
                        orderItem.getId(),
                        order.getId(),
                        orderItem.getBookId(),
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getBook().getTitle(),
                        orderItem.getBook().getCoverUrl()
                ))
                .toList();
        List<PaymentTransactionResponse> payments = order.getPaymentTransactions().stream()
                .sorted(Comparator.comparing(PaymentTransaction::getRequestedAt).reversed())
                .map(paymentTransaction -> new PaymentTransactionResponse(
                        paymentTransaction.getId(),
                        order.getId(),
                        paymentTransaction.getProvider(),
                        paymentTransaction.getPaymentMethod(),
                        paymentTransaction.getTransactionKey(),
                        paymentTransaction.getProviderTransactionId(),
                        paymentTransaction.getAmount(),
                        paymentTransaction.getStatus(),
                        paymentTransaction.getFailureReason(),
                        paymentTransaction.getRequestedAt(),
                        paymentTransaction.getApprovedAt()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getAddress(),
                order.getPaymentMethod(),
                order.getPaymentFailureReason(),
                order.getOrderedAt(),
                order.getPaidAt(),
                items,
                payments
        );
    }
}
