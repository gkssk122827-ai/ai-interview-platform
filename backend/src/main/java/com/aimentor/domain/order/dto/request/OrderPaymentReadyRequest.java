package com.aimentor.domain.order.dto.request;

import jakarta.validation.constraints.Size;

public record OrderPaymentReadyRequest(
        @Size(max = 30, message = "paymentMethod must be 30 characters or fewer.")
        String paymentMethod
) {
}
