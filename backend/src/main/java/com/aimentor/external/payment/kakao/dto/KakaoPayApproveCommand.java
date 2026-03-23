package com.aimentor.external.payment.kakao.dto;

public record KakaoPayApproveCommand(
        Long orderId,
        Long userId,
        String tid,
        String partnerOrderId,
        String partnerUserId,
        String pgToken
) {
}
