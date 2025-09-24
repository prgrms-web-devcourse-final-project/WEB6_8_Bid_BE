package com.backend.domain.cash.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

// 아이템..
public class CashTransactionItemResponse {

    private final Long transactionId; // 원장 PK..
    private final Long cashId;        // 지갑 PK..
    private final String type;        // DEPOSIT / WITHDRAW ..
    private final Long amount;        // 금액(입금=양수, 출금=음수)..
    private final Long balanceAfter;  // 이 줄 반영 후 잔액..
    private final String createdAt;   // 기록 시각(문자열, Z 유지)..

    private final Related related;    // 출처 정보(중첩 객체)..

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Related {
        private final String type;           // PAYMENT, BID ..
        private final Long id;               // 관련 PK..
        private final Product product;       // 상품 정보..
        private final String summary;        // 사람이 읽기 쉬운 요약..
    }

    @Getter
    @Builder
    public static class Product {
        private final Long productId;        // 상품 PK..
        private final String productName;    // 상품 이름..
        private final String thumbnailUrl;   // 썸네일 URL..
    }
}