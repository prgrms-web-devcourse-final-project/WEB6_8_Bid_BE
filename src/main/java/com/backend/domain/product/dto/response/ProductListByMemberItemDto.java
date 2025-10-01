package com.backend.domain.product.dto.response;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.response.component.ReviewDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.ProductCategory;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ProductListByMemberItemDto(
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
        ReviewDto review
) {
    public static ProductListByMemberItemDto fromEntity(Product entity) {
        return new ProductListByMemberItemDto(
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
                ReviewDto.fromEntity(entity.getReview())
        );
    }

    public static ProductListByMemberItemDto fromDocument(ProductDocument document, ReviewDto reviewDto) {
        return new ProductListByMemberItemDto(
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
                reviewDto
        );
    }
}
