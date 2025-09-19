package com.backend.domain.product.enums;

import lombok.Getter;

@Getter
public enum AuctionStatus {
    BEFORE_START("경매 시작 전"),
    BIDDING("경매 중"),
    SUCCESSFUL("낙찰"),
    FAILED("유찰");
    
    private final String displayName;
    
    AuctionStatus(String displayName) {
        this.displayName = displayName;
    }
}