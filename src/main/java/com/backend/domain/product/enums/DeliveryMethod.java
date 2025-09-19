package com.backend.domain.product.enums;

import lombok.Getter;

@Getter
public enum DeliveryMethod {
    DELIVERY("택배"),
    TRADE("직거래"),
    BOTH("택배/직거래");

    private final String displayName;

    DeliveryMethod(String displayName) {
        this.displayName = displayName;
    }
}
