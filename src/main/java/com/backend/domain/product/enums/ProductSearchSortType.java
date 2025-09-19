package com.backend.domain.product.enums;

import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum ProductSearchSortType {
    LATEST("createDate", "DESC", "등록일 최신순"),
    PRICE_HIGH("currentPrice", "DESC", "현재가 높은순"),
    PRICE_LOW("currentPrice", "ASC", "현재가 낮은순"),
    ENDING_SOON("endTime", "ASC", "마감 임박순"),
    POPULAR("bidderCount", "DESC", "인기순");

    private final String fieldName;
    private final String direct;
    private final String description;

    ProductSearchSortType(String fieldName, String direct, String description) {
        this.fieldName = fieldName;
        this.direct = direct;
        this.description = description;
    }

    private Sort.Direction getDirection() {
        return (direct.equals("ASC")) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    public Sort toSort() {
        return Sort.by(getDirection(), fieldName);
    }
}
