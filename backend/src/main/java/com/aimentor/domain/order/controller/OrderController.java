package com.aimentor.domain.order.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.order.dto.request.OrderCreateRequest;
import com.aimentor.domain.order.dto.request.OrderPayRequest;
import com.aimentor.domain.order.dto.request.OrderPaymentApproveRequest;
import com.aimentor.domain.order.dto.request.OrderPaymentReadyRequest;
import com.aimentor.domain.order.dto.response.OrderPaymentReadyResponse;
import com.aimentor.domain.order.dto.response.OrderResponse;
import com.aimentor.domain.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes order APIs scoped to the authenticated user.
 */
@RestController
@RequestMapping({"/api/orders", "/api/v1/orders"})
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return ApiResponse.success(orderService.createOrder(authenticatedUser.userId(), request));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<OrderResponse> payOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) OrderPayRequest request
    ) {
        return ApiResponse.success(orderService.payOrder(authenticatedUser.userId(), orderId, request));
    }

    @PostMapping("/{orderId}/payments/kakao/ready")
    public ApiResponse<OrderPaymentReadyResponse> readyKakaoPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) OrderPaymentReadyRequest request
    ) {
        return ApiResponse.success(orderService.readyKakaoPayment(authenticatedUser.userId(), orderId, request));
    }

    @PostMapping("/{orderId}/payments/kakao/approve")
    public ApiResponse<OrderResponse> approveKakaoPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderPaymentApproveRequest request
    ) {
        return ApiResponse.success(orderService.approveKakaoPayment(authenticatedUser.userId(), orderId, request));
    }

    @PostMapping("/{orderId}/payments/kakao/cancel")
    public ApiResponse<OrderResponse> cancelKakaoPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.cancelKakaoPayment(authenticatedUser.userId(), orderId));
    }

    @PostMapping("/{orderId}/payments/kakao/fail")
    public ApiResponse<OrderResponse> failKakaoPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.failKakaoPayment(authenticatedUser.userId(), orderId));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.success(orderService.getOrders(authenticatedUser.userId()));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.getOrder(authenticatedUser.userId(), orderId));
    }
}
