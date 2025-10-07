package com.backend.domain.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PgChargeResultResponse {
    private final boolean success;        // 승인 성공 여부
    private final String transactionId;   // PG의 paymentKey 등
    private final String failureCode;     // 실패시 코드(선택)
    private final String failureMsg;      // 실패시 메시지(선택)
}
