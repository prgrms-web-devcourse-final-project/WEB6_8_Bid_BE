package com.backend.domain.product.dto.response;

import com.backend.domain.product.dto.response.component.BidderDto;
import com.backend.domain.product.dto.response.component.ReviewDto;
import com.backend.domain.product.entity.Product;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MyProductListItemDto(
        @NotNull Long productId,
        @NotNull String name,
        @NotNull String category,
        @NotNull Long initialPrice,
        @NotNull Long currentPrice,
        @NotNull LocalDateTime auctionStartTime,
        @NotNull LocalDateTime auctionEndTime,
        @NotNull Integer auctionDuration,
        @NotNull String status,
        @NotNull Integer bidderCount,
        String location,
        @NotNull String thumbnailUrl,
        BidderDto bidder,
        ReviewDto review
) {
    public static MyProductListItemDto fromEntity(Product entity) {
        return new MyProductListItemDto(
                entity.getId(),
                entity.getProductName(),
                entity.getCategory().getDisplayName(),
                entity.getInitialPrice(),
                entity.getCurrentPrice(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDuration(),
                entity.getStatus(),
                entity.getBidderCount(),
                entity.getLocation(),
                entity.getThumbnail(),
                BidderDto.fromEntity(entity.getBidder()),
                ReviewDto.fromEntity(entity.getReview())
        );
    }
}
