package com.backend.domain.payment.service;

import com.backend.domain.payment.dto.PgChargeResultResponse;
import com.backend.domain.payment.dto.TossIssueBillingKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossBillingClient {

    @Value("${pg.toss.secretKey}")
    private String secretKey;

    private final WebClient web = WebClient.builder()
            .baseUrl("https://api.tosspayments.com/v1")
            .build();

    // 1) authKey -> billingKey 발급
    public TossIssueBillingKeyResponse issueBillingKey(String customerKey, String authKey) {
        String basic = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = Map.of("customerKey", customerKey);

        try {
            // POST /billing/authorizations/{authKey}
            Map<String, Object> resp = web.post()
                    .uri("/billing/authorizations/{authKey}", authKey)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class).flatMap(err ->
                                    Mono.error(new IllegalStateException(
                                            "Toss error " + r.statusCode() + ": " + err))))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            // 응답에서 필요한 필드 꺼내기 (필드명은 토스 응답 스펙에 맞게)
            // ---- 카드 정보 파싱 ----
            String billingKey = (String) resp.get("billingKey");
            Map<?, ?> card = (Map<?, ?>) resp.get("card");

            String cardNumber = null;
            String cardBrand  = null;
            Integer expMonth  = null;
            Integer expYear   = null;

            if (card != null) {
                // 번호(마스킹일 수 있음) → 마지막 4자리 추출에 사용
                Object number = card.get("number");
                cardNumber = number == null ? null : number.toString();

                // 브랜드/발급사
                // 환경에 따라 company/issuerCode 중 하나가 내려옵니다.
                Object company = card.get("company");
                Object issuerCode = card.get("issuerCode");
                cardBrand = company != null ? company.toString()
                        : issuerCode != null ? issuerCode.toString()
                        : null;

                // 만료월/만료년 (키명이 expire* 또는 expiry* 로 다를 수 있음)
                expMonth = parseInt(card.get("expireMonth"), card.get("expiryMonth"));
                expYear  = parseInt(card.get("expireYear"),  card.get("expiryYear"));
            }

            log.info("[ISSUE] customerKey={}, authKey={}, billingKey={}", customerKey, authKey, billingKey);

            return TossIssueBillingKeyResponse.builder()
                    .billingKey(billingKey)
                    .cardBrand(cardBrand)
                    .cardNumber(cardNumber)
                    .expMonth(expMonth)
                    .expYear(expYear)
                    .build();

        } catch (Exception e) {
            log.error("issueBillingKey failed: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PG 교환 실패: " + e.getMessage());
        }
    }

    private Integer parseInt(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) continue;
            if (c instanceof Number n) return n.intValue();
            try { return Integer.parseInt(c.toString()); } catch (Exception ignore) {}
        }
        return null;
    }



    public PgChargeResultResponse charge(String billingKey, long amount, String idempotencyKey, String customerKey) {
        String basic = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        log.info("[CHARGE->REQ] billingKey={}, customerKey={}, amount={}, idem={}",
                billingKey, customerKey, amount, idempotencyKey);

        Map<String, Object> body = Map.of(
                "amount", amount,
                "orderId", "wallet-" + UUID.randomUUID(), // 고유값
                "orderName", "지갑충전",
                "customerKey", customerKey                 //  발급 때와 동일한 customerKey 필수
        );

        try {
            Map<String, Object> resp = web.post()
                    .uri("/billing/{billingKey}", billingKey) //  올바른 엔드포인트
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r ->
                            r.bodyToMono(String.class).flatMap(b ->
                                    Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "Toss 4xx: " + b))))
                    .onStatus(HttpStatusCode::is5xxServerError, r ->
                            r.bodyToMono(String.class).flatMap(b ->
                                    Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_GATEWAY, "Toss 5xx: " + b))))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            // 성공 시 paymentKey 추출
            String txId = (String) resp.get("paymentKey");
            if (txId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Toss 응답에 paymentKey가 없습니다: " + resp);
            }

            log.info("[CHARGE<-RESP] paymentKey={}", txId);

            return PgChargeResultResponse.builder()
                    .success(true)
                    .transactionId(txId)
                    .build();

        } catch (ResponseStatusException e) {
            // 토스 에러를 그대로 노출 (스택 포함 서버로그)
            log.error("Toss charge error: {}", e.getReason(), e);
            throw e;
        } catch (Exception e) {
            log.error("Toss charge unexpected error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Toss 통신 실패: " + e.getMessage());
        }
    }

}
