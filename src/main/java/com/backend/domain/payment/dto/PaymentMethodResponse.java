package com.backend.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

// 결제 수단 단건/다건 조회..
public class PaymentMethodResponse {

    // 공통..
    private final Long id;
    private final String type;
    private final String aliasName;
    private final Boolean isDefault;
    private final LocalDateTime createDate; // createDate 포함

    // 카드 전용..
    private final String cardCompany;
    private final String cardNumber;
    private final String expireDate;

    // 계좌이체 전용..
    private final String bankName;
    private final String accountNumber;
}