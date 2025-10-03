package com.backend.domain.bid.enums;

import lombok.Getter;

@Getter
public enum BidStatus {
    BIDDING("bidding", "입찰 중"),
    WINNING("winning", "낙찰"),
    LOSING("losing", "낙찰 실패"),
    PAID("paid", "결제 완료"),
    CANCELLED("cancelled", "취소됨");

    private final String value;
    private final String displayName;

    BidStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
}
