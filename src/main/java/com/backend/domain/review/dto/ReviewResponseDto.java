package com.backend.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewResponseDto(
        Long reviewerId,
        String reviewerName,
        Long productId,
        String comment,
        Boolean isSatisfied,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
}
