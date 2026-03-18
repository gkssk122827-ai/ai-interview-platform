package com.aimentor.domain.cart.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.cart.dto.request.CartItemCreateRequest;
import com.aimentor.domain.cart.dto.request.CartItemUpdateRequest;
import com.aimentor.domain.cart.dto.response.CartItemResponse;
import com.aimentor.domain.cart.dto.response.CartResponse;
import com.aimentor.domain.cart.service.CartItemService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes cart APIs scoped to the authenticated user.
 */
@RestController
@RequestMapping({"/api/cart", "/api/v1/cart"})
public class CartItemController {

    private final CartItemService cartItemService;

    public CartItemController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.success(cartItemService.getCart(authenticatedUser.userId()));
    }

    @PostMapping
    public ApiResponse<CartItemResponse> addCartItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CartItemCreateRequest request
    ) {
        return ApiResponse.success(cartItemService.addCartItem(authenticatedUser.userId(), request));
    }

    @PutMapping("/{bookId}")
    public ApiResponse<CartItemResponse> updateCartItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long bookId,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        return ApiResponse.success(cartItemService.updateCartItem(authenticatedUser.userId(), bookId, request));
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> deleteCartItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long bookId
    ) {
        cartItemService.deleteCartItem(authenticatedUser.userId(), bookId);
        return ApiResponse.success();
    }
}
