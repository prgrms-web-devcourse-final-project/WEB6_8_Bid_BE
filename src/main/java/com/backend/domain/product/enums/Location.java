package com.backend.domain.product.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Location {
    SEOUL("서울특별시", "서울"),
    GYEONGGI("경기도", "경기"),
    INCHEON("인천광역시", "인천"),
    BUSAN("부산광역시", "부산"),
    DAEGU("대구광역시", "대구"),
    DAEJEON("대전광역시", "대전"),
    GWANGJU("광주광역시", "광주"),
    ULSAN("울산광역시", "울산"),
    GANGWON("강원도", "강원"),
    CHUNGBUK("충청북도", "충북"),
    CHUNGNAM("충청남도", "충남"),
    JEONBUK("전라북도", "전북"),
    JEONNAM("전라남도", "전남"),
    GYEONGBUK("경상북도", "경북"),
    GYEONGNAM("경상남도", "경남"),
    JEJU("제주도", "제주");
    
    private final String displayName;
    private final String shortName;

    Location(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public static Location fromName(String name) {
        return Arrays.stream(values())
                .filter(location -> location.displayName.equals(name) || location.shortName.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid location: " + name));
    }
}