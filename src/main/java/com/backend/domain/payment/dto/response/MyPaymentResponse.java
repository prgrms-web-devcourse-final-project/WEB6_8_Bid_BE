package com.backend.domain.payment.dto.response;

import com.backend.domain.payment.enums.PaymentMethodType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

// 내 결제 상세 내역..
public class MyPaymentResponse {
    private final Long paymentId;         // 결제 건 번호..
    private final Long paymentMethodId;   // 사용한 결제수단 번호..
    private final String status;          // 결제 상태(SUCCESS 등)..
    private final Long amount;            // 금액(원)..
    private final String provider;        // PG 이름(예: toss)..
    private final PaymentMethodType methodType;      // "CARD"/"BANK"..
    private final String transactionId;   // PG 트랜잭션 ID..
    private final LocalDateTime createdAt;       // 생성/승인 시각..
    private final String idempotencyKey;  // 멱등키..
    private final LocalDateTime paidAt;
    private final Long cashTransactionId; // 원장 '입금' 줄 번호..
    private final Long balanceAfter;      // 이 결제 반영 후 잔액..
}