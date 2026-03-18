package com.aimentor.domain.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemCreateRequest(
        @NotNull(message = "bookId is required.")
        Long bookId,

        @NotNull(message = "quantity is required.")
        @Min(value = 1, message = "quantity must be at least 1.")
        Integer quantity
) {
}
