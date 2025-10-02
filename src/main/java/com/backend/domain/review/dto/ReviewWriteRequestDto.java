package com.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ReviewWriteRequestDto(
        @Schema(description = "상품 ID", example = "1")
        @NotBlank(message = "상품 ID는 필수 입력값 입니다.")
        Long productId,

        @Schema(description = "내용", example = "좋은 거래였습니다")
        @NotBlank(message = "내용은 필수 입력값 입니다.")
        String comment,

        @Schema(description = "만족", example = "만족, 불만족")
        @NotBlank(message = "만족값은 필수 입력값 입니다.")
        Boolean isSatisfied
) {
}

