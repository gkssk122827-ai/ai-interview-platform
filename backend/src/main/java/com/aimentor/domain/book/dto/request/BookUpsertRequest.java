package com.aimentor.domain.book.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Validates book create and update requests.
 */
public record BookUpsertRequest(
        @NotBlank(message = "Book title is required.")
        @Size(max = 200, message = "Book title must be 200 characters or fewer.")
        String title,

        @NotBlank(message = "Author is required.")
        @Size(max = 100, message = "Author must be 100 characters or fewer.")
        String author,

        @NotBlank(message = "Publisher is required.")
        @Size(max = 100, message = "Publisher must be 100 characters or fewer.")
        String publisher,

        @NotNull(message = "Price is required.")
        @DecimalMin(value = "0.00", inclusive = true, message = "Price must be zero or greater.")
        BigDecimal price,

        @NotNull(message = "Stock is required.")
        @Min(value = 0, message = "Stock must be zero or greater.")
        Integer stock,

        @Size(max = 500, message = "Cover URL must be 500 characters or fewer.")
        String coverUrl,

        @Size(max = 5000, message = "Description must be 5000 characters or fewer.")
        String description
) {
}
