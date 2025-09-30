package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TossIssueBillingKeyResponse {
    private String billingKey;
    private String provider;   // "toss"
    private String cardBrand;  // 선택: 스냅샷에 쓰려면
    private String cardNumber; // 선택
    private Integer expMonth;  // 선택
    private Integer expYear;   // 선택
}