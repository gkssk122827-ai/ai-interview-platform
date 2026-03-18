package com.aimentor.domain.order.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long orderId,
        Long bookId,
        Integer quantity,
        BigDecimal price,
        String bookTitle,
        String coverUrl
) {
}
