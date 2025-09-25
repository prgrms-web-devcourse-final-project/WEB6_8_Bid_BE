package com.backend.domain.product.dto;

import com.backend.domain.review.entity.Review;
import jakarta.validation.constraints.NotNull;

public record ReviewDto(
        @NotNull String reviewerNickname,
        @NotNull String productName,
        @NotNull String comment,
        @NotNull Boolean isSatisfied
) {
    public static ReviewDto fromEntity(Review entity) {
        if (entity == null) {
            return null;
        }
        return new ReviewDto(
                entity.getReviewer().getNickname(),
                entity.getProduct().getProductName(),
                entity.getComment(),
                entity.getIsSatisfied()
        );
    }
}
