package com.backend.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

// 내 결제 내역..
public class MyPaymentsResponse {
    private final int page;                              // 현재 페이지(1부터)..
    private final int size;                              // 페이지 크기..
    private final long total;                            // 전체 결제 건수..
    private final List<MyPaymentListItemResponse> items; // 결제 목록 아이템들..
}