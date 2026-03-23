package com.aimentor.domain.order.dto.response;

public record OrderPaymentReadyResponse(
        Long orderId,
        String provider,
        String paymentMethod,
        String transactionKey,
        String redirectUrl
) {
}
