package com.backend.domain.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentMethodEditRequest {
    // 공통..
    private String alias;        // 별명
    private Boolean isDefault;   // 기본 여부
}
