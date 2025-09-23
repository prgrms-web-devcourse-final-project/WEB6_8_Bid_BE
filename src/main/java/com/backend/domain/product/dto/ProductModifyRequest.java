package com.backend.domain.product.dto;

import com.backend.domain.product.enums.DeliveryMethod;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record ProductModifyRequest(
        @Size(min = 1, max = 100, message = "상품명은 1~100자로 입력해주세요.")
        String name,

        @Size(max = 1000, message = "상품 설명은 1000자 이하로 입력해주세요.")
        String description,

        @Min(value = 1, message = "올바른 카테고리를 선택해주세요.")
        @Max(value = 12, message = "올바른 카테고리를 선택해주세요.")
        Integer categoryId,

        @Min(value = 1000, message = "시작가는 1,000원 이상이어야 합니다.")
        Long initialPrice,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime auctionStartTime,

        @Pattern(regexp = "^(24시간|48시간)$", message = "경매 진행 시간은 24시간 또는 48시간만 가능합니다.")
        String auctionDuration,

        DeliveryMethod deliveryMethod,

        String location
) {}
