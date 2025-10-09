package com.backend.domain.cash.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
// 원장 목록 + 페이징..
public class CashTransactionsResponse {
    private final int page;                              // 현재 페이지..
    private final int size;                              // 페이지 크기..
    private final long total;                            // 총 개수..
    private final List<CashTransactionItemResponse> items; // 원장 아이템들..
}
