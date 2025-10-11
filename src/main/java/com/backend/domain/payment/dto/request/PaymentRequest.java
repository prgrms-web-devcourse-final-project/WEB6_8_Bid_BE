package com.backend.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(
            description = "멱등키(재시도 시 동일하게 사용)",
            example = "d6a6f3ad-5d9a-4a3a-b0a5-7e0a2b77c2b1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String idempotencyKey; // 멱등키(재시도 동일키)..
}