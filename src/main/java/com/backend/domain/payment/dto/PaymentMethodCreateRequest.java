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
}