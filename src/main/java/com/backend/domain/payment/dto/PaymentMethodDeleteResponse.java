package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

// 결제 수단 삭제..
public class PaymentMethodDeleteResponse {
    private final Long id;
    private final boolean deleted;
}