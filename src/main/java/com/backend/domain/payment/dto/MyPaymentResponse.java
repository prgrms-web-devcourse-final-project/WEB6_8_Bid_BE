package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

// 내 결제 상세 내역 조회..
public class MyPaymentResponse {
    private final Long id;
    private final Long bidId;
    private final String productName;
    private final int amount;
    private final String sellerNickname;
    private final String status;
    private final PaymentMethodInPaymentResponse paymentMethod;
    private final LocalDateTime paidDate;
    private final String thumbnailUrl;
}