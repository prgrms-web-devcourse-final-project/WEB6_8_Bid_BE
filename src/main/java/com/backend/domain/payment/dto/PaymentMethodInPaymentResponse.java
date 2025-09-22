package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder

// 결제 상세 내역 조회 응답에 포함될 결제 수단 정보..
public class PaymentMethodInPaymentResponse {
    private final String type;
    private final String aliasName;
    private final String cardCompany;
    private final String cardNumber;
}