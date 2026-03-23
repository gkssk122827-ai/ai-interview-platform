package com.aimentor.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderPaymentApproveRequest(
        @NotBlank(message = "pgToken is required.")
        String pgToken
) {
}
