package com.aimentor.domain.cart.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResponse(
        Long id,
        Long userId,
        Long bookId,
        Integer quantity,
        String bookTitle,
        String bookAuthor,
        String bookPublisher,
        BigDecimal bookPrice,
        String coverUrl,
        BigDecimal linePrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
