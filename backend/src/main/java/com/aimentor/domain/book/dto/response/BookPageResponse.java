package com.aimentor.domain.book.dto.response;

import java.util.List;

/**
 * Wraps paginated book search results.
 */
public record BookPageResponse(
        List<BookResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
