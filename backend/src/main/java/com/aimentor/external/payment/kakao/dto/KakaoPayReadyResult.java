package com.aimentor.external.payment.kakao.dto;

public record KakaoPayReadyResult(
        String tid,
        String redirectUrl
) {
}
