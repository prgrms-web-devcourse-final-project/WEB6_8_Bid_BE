package com.backend.domain.product.dto.response;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.response.component.BidderDto;
import com.backend.domain.product.dto.response.component.ReviewDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.ProductCategory;
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

    public static MyProductListItemDto fromDocument(ProductDocument document, BidderDto bidderDto, ReviewDto reviewDto) {
        return new MyProductListItemDto(
                document.getProductId(),
                document.getProductName(),
                ProductCategory.fromName(document.getCategory()),
                document.getInitialPrice(),
                document.getCurrentPrice(),
                document.getStartTime(),
                document.getEndTime(),
                document.getDuration(),
                document.getStatus(),
                document.getBidderCount(),
                document.getLocation(),
                document.getThumbnailUrl(),
                bidderDto,
                reviewDto
        );
    }
}
