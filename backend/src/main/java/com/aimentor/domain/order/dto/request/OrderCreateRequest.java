package com.aimentor.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderCreateRequest(
        @NotBlank(message = "address is required.")
        String address
) {
}
