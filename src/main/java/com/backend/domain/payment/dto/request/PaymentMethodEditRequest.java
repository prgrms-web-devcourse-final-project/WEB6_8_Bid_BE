package com.backend.domain.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentMethodEditRequest {
    // 공통..
    private String alias;        // 별명
    private Boolean isDefault;   // 기본 여부

    // CARD 전용(해당 타입일 때만 반영)..
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;

    // BANK 전용(해당 타입일 때만 반영)..
    private String bankCode;     // 선택
    private String bankName;
    private String acctLast4;
}
