package com.backend.domain.product.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ProductCategory {
    DIGITAL_ELECTRONICS(1, "디지털/가전"),
    FASHION_CLOTHING(2, "패션/의류"),
    BEAUTY(3, "뷰티/미용"),
    HOME_LIVING(4, "홈/리빙"),
    SPORTS_LEISURE(5, "스포츠/레저"),
    BOOKS_MEDIA(6, "도서/음반/DVD"),
    PET_SUPPLIES(7, "반려동물용품"),
    BABY_KIDS(8, "유아동/출산용품"),
    FOOD_HEALTH(9, "식품/건강식품"),
    AUTOMOTIVE(10, "자동차/오토바이"),
    HOBBY_COLLECTIBLES(11, "취미/수집품"),
    ETC(12, "기타");

    private final int id;
    private final String displayName;

    ProductCategory(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    // ID로 enum 찾기
    public static ProductCategory fromId(int id) {
        return Arrays.stream(values())
                .filter(category -> category.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + id));
    }
}