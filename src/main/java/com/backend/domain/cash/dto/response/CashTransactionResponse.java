package com.backend.domain.cash.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

// 원장 단건 상세 data..
public class CashTransactionResponse {
    private final Long transactionId;   // CashTransactions.id..
    private final Long cashId;          // CashTransactions.cash_id..
    private final String type;          // DEPOSIT / WITHDRAW ..
    private final Long amount;          // 금액..
    private final Long balanceAfter;    // 이 줄 반영 후 잔액..
    private final String createdAt;     // 기록 시각..

    private final Related related;      // 연결 출처..

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Related {
        private final String type;      // PAYMENT | BID ..
        private final Long id;          // paymentId 또는 bidId..
        private final Links links;      // 바로가기 링크들..
    }

    @Getter
    @Builder
    public static class Links {
        private final String paymentDetail; // 예: "/payments/me/101"
        private final String bidDetail;     // 예: /api/v1/bids/products/{productId}
    }
}
