package com.backend.domain.payment.dto;


import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@Builder

// 결제 요청..
public class PaymentResponse {
    private final Long productId;
    private final int amount;
    private final PaymentMethodInPaymentResponse paymentMethod;
    private final String status;
    private final LocalDateTime paidDate;
}