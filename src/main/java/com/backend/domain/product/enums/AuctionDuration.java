package com.backend.domain.product.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AuctionDuration {
    HOURS_24("24시간", 24),
    HOURS_48("48시간", 48);
    
    private final String value;
    private final int hours;
    
    AuctionDuration(String value, int hours) {
        this.value = value;
        this.hours = hours;
    }
    
    // 문자열로 Integer 찾기
    public static Integer fromValue(String value) {
        return Arrays.stream(values())
                .filter(duration -> duration.value.equals(value))
                .findFirst()
                .map(AuctionDuration::getHours)
                .orElseThrow(() -> new IllegalArgumentException("Invalid duration: " + value));
    }
}