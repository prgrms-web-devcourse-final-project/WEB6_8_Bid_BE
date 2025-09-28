package com.backend.domain.product.dto.request;

import com.backend.domain.product.enums.DeliveryMethod;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "상품 등록 요청")
public record ProductCreateRequest(
        @Schema(description = "상품명", example = "아이폰 15 Pro 256GB")
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(min = 1, max = 100, message = "상품명은 1~100자로 입력해주세요.")
        String name,

        @Schema(description = "상품 설명", example = "미개봉 새 제품입니다. 직거래 선호합니다.")
        @Size(max = 1000, message = "상품 설명은 1000자 이하로 입력해주세요.")
        String description,

        @Schema(description = "카테고리 ID", example = "1")
        @NotNull(message = "카테고리는 필수입니다.")
        @Min(value = 1, message = "올바른 카테고리를 선택해주세요.")
        @Max(value = 12, message = "올바른 카테고리를 선택해주세요.")
        Integer categoryId,

        @Schema(description = "시작 가격", example = "1000000")
        @NotNull(message = "시작가는 필수입니다.")
        @Min(value = 1000, message = "시작가는 1,000원 이상이어야 합니다.")
        Long initialPrice,

        @Schema(description = "경매 시작 시간", example = "2025-09-23T09:00:00")
        @NotNull(message = "경매 시작 시간은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime auctionStartTime,

        @Schema(description = "경매 기간", example = "24시간")
        @NotNull(message = "경매 진행 시간은 필수입니다.")
        @Pattern(regexp = "^(24시간|48시간)$", message = "경매 진행 시간은 24시간 또는 48시간만 가능합니다.")
        String auctionDuration,

        @Schema(description = "배송 방법", example = "TRADE")
        @NotBlank(message = "배송 방법은 필수입니다.")
        DeliveryMethod deliveryMethod,

        @Schema(description = "위치", example = "서울 강남구")
        String location
) {}
