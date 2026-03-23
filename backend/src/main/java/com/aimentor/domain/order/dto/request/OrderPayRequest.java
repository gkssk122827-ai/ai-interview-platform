package com.aimentor.domain.order.dto.request;

import jakarta.validation.constraints.Size;

public record OrderPayRequest(
        @Size(max = 30, message = "paymentMethod must be 30 characters or fewer.")
        String paymentMethod,

        Boolean success,

        @Size(max = 500, message = "failureReason must be 500 characters or fewer.")
        String failureReason
) {
}
