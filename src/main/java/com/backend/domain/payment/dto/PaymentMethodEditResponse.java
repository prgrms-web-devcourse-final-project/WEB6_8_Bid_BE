package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 결제 수단 수정..
public class PaymentMethodEditResponse {
    private final Long id;           // 수정된 결제수단 PK..
    private final String alias;      // 변경된 별명..
    private final Boolean isDefault; // 변경된 기본 여부..
    private final String modifyDate; // 수정 시각..
}