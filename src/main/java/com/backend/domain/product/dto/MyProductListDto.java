package com.backend.domain.product.dto;

import com.backend.domain.product.entity.Product;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MyProductListDto(
        @NotNull Long productId,
        @NotNull String name,
        @NotNull String category,
        @NotNull Long initialPrice,
        @NotNull Long currentPrice,
        @NotNull LocalDateTime auctionStartTime,
        @NotNull LocalDateTime auctionEndTime,
        @NotNull Integer auctionDuration,
        @NotNull String status,
//        @NotNull Long biddersCount,
        String location,
        @NotNull String thumbnailUrl,
        BidderDto bidder,
        ReviewDto review
) {
    public static MyProductListDto fromEntity(Product entity) {
        return new MyProductListDto(
                entity.getId(),
                entity.getProductName(),
                entity.getCategory().getDisplayName(),
                entity.getInitialPrice(),
                entity.getCurrentPrice(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDuration(),
                entity.getStatus(),
//                entity.getBiddersCount(),
                entity.getLocation(),
                entity.getThumbnail(),
                BidderDto.fromEntity(entity.getBidder()),
                ReviewDto.fromEntity(entity.getReview())
        );
    }
}
