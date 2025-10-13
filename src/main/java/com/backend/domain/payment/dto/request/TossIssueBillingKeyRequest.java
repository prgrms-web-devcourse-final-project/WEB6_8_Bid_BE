package com.backend.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TossIssueBillingKeyRequest {
    @Schema(description = "Toss가 successUrl로 전달한 키", example = "bln_xxx")
    private String authKey;
}