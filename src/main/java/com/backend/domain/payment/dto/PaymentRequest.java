package com.backend.domain.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor

// 결제 요청..
public class PaymentRequest {
    private Long bidId;
    private Long paymentMethodId;
    private int amount;
}