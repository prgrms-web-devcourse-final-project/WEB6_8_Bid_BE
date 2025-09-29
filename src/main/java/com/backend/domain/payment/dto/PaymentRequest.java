package com.backend.domain.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 결제 요청..
@Getter
@Setter
public class PaymentRequest {

    @NotNull
    private Long paymentMethodId;  // 결제수단 ID..

    @NotNull
    @Min(100)
    private Long amount;           // 충전 금액(원)..

    @NotBlank
    private String idempotencyKey; // 멱등키(재시도 동일키)..
}