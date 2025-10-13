package com.backend.domain.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TossIssueBillingKeyResponse {
    @Schema(description = "Toss 토큰(우리 쪽 저장용)", example = "PAhFKE3...")
    private String billingKey;

    private String provider;   // "toss"

    @Schema(description = "카드 브랜드/발급사", example = "SHINHAN")
    private String cardBrand;  // 선택: 스냅샷에 쓰려면

    @Schema(description = "카드 끝 4자리", example = "1234")
    private String last4; // 선택

    @Schema(description = "만료월", example = "12")
    private Integer expMonth;  // 선택

    @Schema(description = "만료년", example = "2028")
    private Integer expYear;   // 선택
}