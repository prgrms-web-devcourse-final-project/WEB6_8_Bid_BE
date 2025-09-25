package com.backend.domain.product.enums;

import lombok.Getter;

@Getter
public enum SaleStatus {
    SELLING("판매 중"),
    SOLD("판매 완료"),
    FAILED("유찰");

    private final String displayName;

    SaleStatus(String displayName) {
        this.displayName = displayName;
    }

    public static AuctionStatus fromSaleStatus(SaleStatus saleStatus) {
        if (saleStatus == SELLING) return AuctionStatus.BIDDING;
        else if (saleStatus == SOLD) return AuctionStatus.SUCCESSFUL;
        else if (saleStatus == FAILED) return AuctionStatus.FAILED;
        else throw new IllegalArgumentException("Invalid SaleStatus: " + saleStatus);
    }
}