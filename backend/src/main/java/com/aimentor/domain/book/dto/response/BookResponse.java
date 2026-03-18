package com.aimentor.domain.book.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Returns book information to clients.
 */
public record BookResponse(
        Long id,
        String title,
        String author,
        String publisher,
        BigDecimal price,
        Integer stock,
        String coverUrl,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
