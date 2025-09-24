package com.backend.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

// 내 결제 내역..
public class MyPaymentListItemResponse {
    private final Long paymentId;        // 결제 건 번호..
    private final Long paymentMethodId;  // 사용한 결제수단 번호..
    private final String status;         // SUCCESS / FAILED / ...
    private final Long amount;           // 금액(원)..
    private final String currency;       // "KRW"..
    private final String provider;       // PG 이름(예: toss)..
    private final String methodType;     // "CARD"/"BANK"..
    private final String transactionId;  // PG 트랜잭션 ID(성공건만 존재, 실패면 null)..
    private final String createdAt;      // 생성/승인 시각(문자열, Z 유지)..
}