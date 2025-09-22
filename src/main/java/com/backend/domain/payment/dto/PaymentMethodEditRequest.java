package com.backend.domain.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;



@Getter
@NoArgsConstructor

// 결제 수단 수정..
public class PaymentMethodEditRequest {
    private String aliasName;
    private Boolean isDefault;
}