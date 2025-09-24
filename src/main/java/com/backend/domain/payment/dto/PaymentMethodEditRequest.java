package com.backend.domain.payment.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
// 결제 수단 수정..
public class PaymentMethodEditRequest {
    private String alias;       // 변경할 별명..
    private Boolean isDefault;  // 기본 여부 변경..
}