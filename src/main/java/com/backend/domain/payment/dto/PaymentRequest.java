package com.backend.domain.payment.dto;

import lombok.Getter;
import lombok.Setter;

// 결제 요청..
@Getter
@Setter
public class PaymentRequest {
    private Long paymentMethodId;  // 결제수단 ID..
    private Long amount;           // 충전 금액(원)..
    private String currency;       // 통화(KRW)..
    private String idempotencyKey; // 멱등키..
}