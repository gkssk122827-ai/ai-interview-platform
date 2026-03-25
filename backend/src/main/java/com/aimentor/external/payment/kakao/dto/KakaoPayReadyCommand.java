package com.aimentor.external.payment.kakao.dto;

import java.math.BigDecimal;

public record KakaoPayReadyCommand(
        Long orderId,
        Long userId,
        String partnerOrderId,
        String partnerUserId,
        String itemName,
        int quantity,
        BigDecimal totalAmount,
        String approvalUrl,
        String cancelUrl,
        String failUrl
) {
}
