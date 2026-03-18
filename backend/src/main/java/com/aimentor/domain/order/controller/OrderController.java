package com.aimentor.domain.order.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.order.dto.request.OrderCreateRequest;
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
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.payOrder(authenticatedUser.userId(), orderId));
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
