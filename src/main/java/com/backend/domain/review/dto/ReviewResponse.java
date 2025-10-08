package com.backend.domain.review.dto;

import com.backend.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        String reviewerNickname,
        String comment,
        Boolean isSatisfied,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getReviewer().getNickname(),
                review.getComment(),
                review.getIsSatisfied(),
                review.getCreateDate()
        );
    }
}