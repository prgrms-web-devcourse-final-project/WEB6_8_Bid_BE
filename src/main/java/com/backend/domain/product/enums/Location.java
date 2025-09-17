package com.backend.domain.product.enums;

import java.util.Arrays;

public enum Location {
    SEOUL("서울"),
    GYEONGGI("경기도"),
    INCHEON("인천"),
    BUSAN("부산"),
    DAEGU("대구"),
    DAEJEON("대전"),
    GWANGJU("광주"),
    ULSAN("울산"),
    GANGWON("강원도"),
    CHUNGBUK("충청북도"),
    CHUNGNAM("충청남도"),
    JEONBUK("전라북도"),
    JEONNAM("전라남도"),
    GYEONGBUK("경상북도"),
    GYEONGNAM("경상남도"),
    JEJU("제주도");
    
    private final String displayName;

    Location(String displayName) {
        this.displayName = displayName;
    }
    
    public static Location fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(location -> location.displayName.equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid location: " + displayName));
    }
}