package com.backend.domain.payment.dto.response;

import com.backend.domain.payment.enums.PaymentMethodType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

// 내 결제 내역..
public class MyPaymentListItemResponse {
    private final Long paymentId;        // 결제 건 번호..
    private final String status;         // SUCCESS / FAILED / ...
    private final Long amount;           // 금액(원)..
    private final String provider;       // PG 이름(예: toss)..
    private final PaymentMethodType methodType;     // "CARD"/"BANK"..
    private final LocalDateTime createdAt;      // 생성/승인 시각(문자열, Z 유지)..
    private final Long cashTransactionId; // 연결된 지갑 원장 ID(성공 시)
    private final Long balanceAfter;      // 충전 후 잔액(성공 시)
}