package com.backend.domain.payment.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TossConfirmResultResponse {
    String billingKey;   // 토스에서 최종 발급되는 billingKey (DB에 token으로 저장)
    String cardBrand;    // 예: "KB", "Hyundai" ...
    String last4;        // 카드번호 끝 4자리
    Integer expMonth;    // 유효기간 MM
    Integer expYear;     // 유효기간 YYYY
}