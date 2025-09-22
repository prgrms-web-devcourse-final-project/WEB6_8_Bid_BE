package com.backend.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

// 결제 수단 수정..
public class PaymentMethodEditResponse {
    private final Long id;
    private final String type;
    private final String aliasName;
    private final String cardCompany;
    private final String cardNumber;
    private final String expireDate;
    private final Boolean isDefault;
    private final LocalDateTime createDate;
    private final LocalDateTime modifyDate;
}