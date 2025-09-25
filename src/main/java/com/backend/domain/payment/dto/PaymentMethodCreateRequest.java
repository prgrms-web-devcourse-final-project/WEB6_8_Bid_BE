package com.backend.domain.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

// 결제 수단 등록..
public class PaymentMethodCreateRequest {
    private String type;       // "CARD" 또는 "BANK"...
    private String token;      // 결제사 토큰..
    private String alias;      // 별명..
    private Boolean isDefault; // 기본 수단 여부..

    // 카드 전용..
    private String brand;       // 예: SHINHAN
    private String last4;       // "1234"
    private Integer expMonth;   // 1~12
    private Integer expYear;    // 예: 2028

    // 계좌 전용..
    private String bankCode;    // 예: 004
    private String bankName;    // 예: KB국민은행
    private String acctLast4;   // "5678"
}