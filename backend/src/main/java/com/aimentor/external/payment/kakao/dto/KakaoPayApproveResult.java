package com.aimentor.external.payment.kakao.dto;

import java.time.LocalDateTime;

public record KakaoPayApproveResult(
        String tid,
        String aid,
        String paymentMethodType,
        LocalDateTime approvedAt
) {
}
