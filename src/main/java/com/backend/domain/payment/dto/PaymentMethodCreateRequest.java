package com.backend.domain.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;



@Getter
@NoArgsConstructor

// 결제 수단 등록..
public class PaymentMethodCreateRequest {

    // 공통..
    private String type;
    private String aliasName;
    private Boolean isDefault;

    // 신용카드..
    private String cardCompany;
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    // 계좌이체..
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
}