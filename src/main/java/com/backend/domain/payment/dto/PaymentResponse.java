package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

// 결제 요청..
public class PaymentResponse {
    private final Long paymentId;         // 결제 PK..
    private final Long paymentMethodId;   // 사용한 결제수단 ID..
    private final String status;          // SUCCESS 등..
    private final Long amount;            // 충전 금액(원)..
    private final String currency;        // "KRW"..
    private final String provider;        // 예: "toss"..
    private final String methodType;      // "CARD"/"BANK"..
    private final String transactionId;   // PG 트랜잭션 ID..
    private final LocalDateTime createdAt;       // 생성/승인 시각(문자열, Z 유지)..
    private final LocalDateTime paidAt;          // 성공 승인 시각..
    private final String idempotencyKey;  // 멱등키(그대로 에코)..

    private final Long cashTransactionId; // 원장 ID(입금 줄)..
    private final Long balanceAfter;      // 충전 후 잔액..
}