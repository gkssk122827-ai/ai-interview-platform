package com.aimentor.external.payment.kakao;

import com.aimentor.common.exception.ApiException;
import com.aimentor.external.payment.kakao.dto.KakaoPayApproveCommand;
import com.aimentor.external.payment.kakao.dto.KakaoPayApproveResult;
import com.aimentor.external.payment.kakao.dto.KakaoPayReadyCommand;
import com.aimentor.external.payment.kakao.dto.KakaoPayReadyResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class KakaoPayService {

    private static final Logger log = LoggerFactory.getLogger(KakaoPayService.class);

    private final KakaoPayProperties properties;
    private final RestTemplate restTemplate;

    public KakaoPayService(KakaoPayProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMillis());
        requestFactory.setReadTimeout(properties.readTimeoutMillis());
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public KakaoPayReadyResult ready(KakaoPayReadyCommand command) {
        validateConfigured();

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(buildReadyBody(command), createHeaders());

        try {
            log.debug(
                    "[KakaoPay][Ready] orderId={}, contentType={}",
                    command.orderId(),
                    requestEntity.getHeaders().getContentType()
            );

            ResponseEntity<KakaoPayReadyApiResponse> response = restTemplate.exchange(
                    properties.baseUrl() + "/v1/payment/ready",
                    HttpMethod.POST,
                    requestEntity,
                    KakaoPayReadyApiResponse.class
            );

            KakaoPayReadyApiResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.tid()) || !StringUtils.hasText(body.nextRedirectPcUrl())) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PAY_INVALID_READY_RESPONSE", "카카오페이 준비 응답이 올바르지 않습니다.");
            }

            log.info("[KakaoPay][Ready] orderId={}, userId={}, tid={}", command.orderId(), command.userId(), maskValue(body.tid()));
            return new KakaoPayReadyResult(body.tid(), body.nextRedirectPcUrl());
        } catch (RestClientResponseException ex) {
            log.error("[KakaoPay][Ready] orderId={}, status={}, body={}", command.orderId(), ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PAY_READY_FAILED", "카카오페이 결제 준비에 실패했습니다.");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[KakaoPay][Ready] orderId={}, message={}", command.orderId(), ex.getMessage(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PAY_READY_FAILED", "카카오페이 결제 준비 중 오류가 발생했습니다.");
        }
    }

    public KakaoPayApproveResult approve(KakaoPayApproveCommand command) {
        validateConfigured();

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(buildApproveBody(command), createHeaders());

        try {
            log.debug(
                    "[KakaoPay][Approve] orderId={}, contentType={}",
                    command.orderId(),
                    requestEntity.getHeaders().getContentType()
            );

            ResponseEntity<KakaoPayApproveApiResponse> response = restTemplate.exchange(
                    properties.baseUrl() + "/v1/payment/approve",
                    HttpMethod.POST,
                    requestEntity,
                    KakaoPayApproveApiResponse.class
            );

            KakaoPayApproveApiResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.tid())) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PAY_INVALID_APPROVE_RESPONSE", "카카오페이 승인 응답이 올바르지 않습니다.");
            }

            log.info("[KakaoPay][Approve] orderId={}, userId={}, tid={}", command.orderId(), command.userId(), maskValue(body.tid()));
            return new KakaoPayApproveResult(
                    body.tid(),
                    body.aid(),
                    body.paymentMethodType(),
                    parseApprovedAt(body.approvedAt())
            );
        } catch (RestClientResponseException ex) {
            log.error("[KakaoPay][Approve] orderId={}, status={}, body={}", command.orderId(), ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PAY_APPROVE_FAILED", "카카오페이 승인에 실패했습니다.");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[KakaoPay][Approve] orderId={}, message={}", command.orderId(), ex.getMessage(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PAY_APPROVE_FAILED", "카카오페이 승인 중 오류가 발생했습니다.");
        }
    }

    private void validateConfigured() {
        boolean hasAdminKey = StringUtils.hasText(properties.adminKey());
        boolean hasCid = StringUtils.hasText(properties.cid());
        boolean hasBaseUrl = StringUtils.hasText(properties.baseUrl());
        boolean hasClientBaseUrl = StringUtils.hasText(properties.clientBaseUrl());

        log.info(
                "[KakaoPay][Config] enabled={}, hasAdminKey={}, hasCid={}, hasBaseUrl={}, hasClientBaseUrl={}",
                properties.enabled(),
                hasAdminKey,
                hasCid,
                hasBaseUrl,
                hasClientBaseUrl
        );

        if (!properties.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "KAKAO_PAY_DISABLED", "카카오페이 결제가 비활성화되어 있습니다. KAKAO_PAY_ENABLED 설정을 확인해 주세요.");
        }
        if (!hasAdminKey || !hasCid || !hasBaseUrl || !hasClientBaseUrl) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "KAKAO_PAY_NOT_CONFIGURED",
                    "카카오페이 환경변수가 누락되었습니다. KAKAO_ADMIN_KEY, KAKAO_PAY_CID, KAKAO_PAY_CLIENT_BASE_URL 설정을 확인해 주세요."
            );
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.adminKey());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private MultiValueMap<String, String> buildReadyBody(KakaoPayReadyCommand command) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("cid", properties.cid());
        body.add("partner_order_id", command.partnerOrderId());
        body.add("partner_user_id", command.partnerUserId());
        body.add("item_name", command.itemName());
        body.add("quantity", String.valueOf(command.quantity()));
        body.add("total_amount", String.valueOf(toKakaoAmount(command.totalAmount())));
        body.add("tax_free_amount", "0");
        body.add("approval_url", command.approvalUrl());
        body.add("cancel_url", command.cancelUrl());
        body.add("fail_url", command.failUrl());
        return body;
    }

    private MultiValueMap<String, String> buildApproveBody(KakaoPayApproveCommand command) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("cid", properties.cid());
        body.add("tid", command.tid());
        body.add("partner_order_id", command.partnerOrderId());
        body.add("partner_user_id", command.partnerUserId());
        body.add("pg_token", command.pgToken());
        return body;
    }

    private int toKakaoAmount(BigDecimal amount) {
        return amount.setScale(0, java.math.RoundingMode.DOWN).intValueExact();
    }

    private LocalDateTime parseApprovedAt(String approvedAt) {
        if (!StringUtils.hasText(approvedAt)) {
            return LocalDateTime.now();
        }

        try {
            return OffsetDateTime.parse(approvedAt).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return LocalDateTime.now();
        }
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 6) {
            return value;
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }

    private record KakaoPayReadyApiResponse(
            String tid,
            @JsonProperty("next_redirect_pc_url")
            String nextRedirectPcUrl
    ) {
    }

    private record KakaoPayApproveApiResponse(
            String aid,
            String tid,
            @JsonProperty("payment_method_type")
            String paymentMethodType,
            @JsonProperty("approved_at")
            String approvedAt
    ) {
    }
}
