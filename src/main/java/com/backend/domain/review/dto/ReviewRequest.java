package com.backend.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @NotBlank(message = "리뷰 내용은 필수입니다.")
        String comment,

        @NotNull(message = "만족 여부는 필수입니다.")
        Boolean isSatisfied
) {
}