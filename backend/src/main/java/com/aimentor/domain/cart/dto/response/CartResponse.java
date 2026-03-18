package com.aimentor.domain.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        Integer totalQuantity,
        BigDecimal totalPrice
) {
}
