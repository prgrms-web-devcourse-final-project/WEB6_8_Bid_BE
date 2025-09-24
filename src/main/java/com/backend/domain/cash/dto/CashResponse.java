package com.backend.domain.cash.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 내 지갑 잔액 응답 data..
public class CashResponse {
    private final Long cashId;        // 지갑 PK..
    private final Long memberId;      // 회원 PK..
    private final Long balance;       // 현재 잔액(원)..
    private final String createDate;  // 생성 시각..
    private final String modifyDate;  // 최근 갱신 시각..
}