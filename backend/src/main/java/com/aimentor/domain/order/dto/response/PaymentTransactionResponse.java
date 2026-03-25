package com.aimentor.domain.order.dto.response;

import com.aimentor.domain.order.entity.PaymentTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        Long id,
        Long orderId,
        String provider,
        String paymentMethod,
        String transactionKey,
        String providerTransactionId,
        BigDecimal amount,
        PaymentTransactionStatus status,
        String failureReason,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt
) {
}
