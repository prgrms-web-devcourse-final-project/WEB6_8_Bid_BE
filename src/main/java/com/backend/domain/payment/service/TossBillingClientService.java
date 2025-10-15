package com.backend.domain.payment.service;

import com.backend.domain.payment.dto.response.PgChargeResultResponse;
import com.backend.domain.payment.dto.response.TossIssueBillingKeyResponse;
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
public class TossBillingClientService {

    @Value("${pg.toss.secretKey}")
    private String secretKey;

    // 웹으로 요청을 보내는 도구 준비..
    private final WebClient web = WebClient.builder()

            // 토스 API 기본 주소..
            .baseUrl("https://api.tosspayments.com/v1")
            .build();

    // authKey -> billingKey 발급
    // 앱에서 카드 인증을 마치면 authKey를 얻음 -> 이걸 토스에게 보내서 billingKey(카드 토큰)를 받음..
    public TossIssueBillingKeyResponse issueBillingKey(String customerKey, String authKey) {
        String basic = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = Map.of("customerKey", customerKey);

        try {
            // POST /billing/authorizations/{authKey} 로 보내면 토스가 billingKey를 줌..
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
            String billingKey = (String) resp.get("billingKey"); // 카드 토큰..
            Map<?, ?> card = (Map<?, ?>) resp.get("card");

            String cardBrand  = null;
            String last4      = null;
            Integer expMonth  = null;
            Integer expYear   = null;

            if (card != null) {
                // 번호(마스킹일 수 있음) → 마지막 4자리 추출에 사용
                Object company    = card.get("company");
                Object issuerCode = card.get("issuerCode");
                cardBrand = company != null ? company.toString()
                        : issuerCode != null ? issuerCode.toString()
                        : null;

                // 브랜드/발급사
                // 환경에 따라 company/issuerCode 중 하나가 내려옵니다.
                Object number = card.get("number"); // "1234-****-****-5678" 등
                if (number != null) {
                    String digits = number.toString().replaceAll("\\D", "");
                    if (digits.length() >= 4) last4 = digits.substring(digits.length() - 4);
                }

                // 만료월/만료년 (키명이 expire* 또는 expiry* 로 다를 수 있음)
                expMonth = parseInt(card.get("expireMonth"), card.get("expiryMonth"));
                expYear  = parseInt(card.get("expireYear"),  card.get("expiryYear"));
                if (expYear != null && expYear < 100) expYear = 2000 + expYear;
            }

            log.info("[ISSUE] customerKey={}, authKey={}, billingKey={}", customerKey, authKey, billingKey);

            if (billingKey == null) {
                throw new IllegalStateException("Toss 응답에 billingKey가 없습니다.");
            }

            return TossIssueBillingKeyResponse.builder()
                    .billingKey(billingKey)
                    .brand(cardBrand)
                    .last4(last4)
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


    // billingKey로 실제 결제(충전), 이제 진짜로 돈을 결제(지갑 충전)해 달라고 토스에 부탁함..
    public PgChargeResultResponse charge(String billingKey, long amount, String idempotencyKey, String customerKey) {
        String basic = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        log.info("[CHARGE->REQ] billingKey={}, customerKey={}, amount={}, idem={}",
                billingKey, customerKey, amount, idempotencyKey);

        // 토스가 필요로 하는 결제 정보..
        Map<String, Object> body = Map.of(
                "amount", amount,
                "orderId", "wallet-" + UUID.randomUUID(), // 고유값
                "orderName", "지갑충전",
                "customerKey", customerKey                   //  발급 때와 동일한 customerKey 필수
        );

        try {
            // POST /billing/{billingKey} 로 결제 요청..
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
            // 토스 에러를 그대로 전달..
            log.error("Toss charge error: {}", e.getReason(), e);
            throw e;
        } catch (Exception e) {
            log.error("Toss charge unexpected error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Toss 통신 실패: " + e.getMessage());
        }
    }

}
