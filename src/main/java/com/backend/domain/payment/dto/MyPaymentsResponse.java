package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
@Builder

// 내 결제 내역 data 객체를 나타내는..
public class MyPaymentsResponse {
    private final List<MyPaymentItemResponse> content;
    private final PageableResponse pageable;
}