package com.backend.domain.product.dto.response;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.response.component.SellerDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.ProductCategory;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ProductListItemDto(
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
        @NotNull SellerDto seller
) {
    public static ProductListItemDto fromEntity(Product entity) {
        return new ProductListItemDto(
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
                SellerDto.fromEntity(entity.getSeller())
        );
    }

    public static ProductListItemDto fromDocument(ProductDocument document) {
        return new ProductListItemDto(
                document.getProductId(),
                document.getProductName(),
                ProductCategory.fromName(document.getCategory()),
                document.getInitialPrice(),
                document.getCurrentPrice(),
                document.getStartTime(),
                document.getEndTime(),
                document.getDuration(),
                document.getStatus(),
//                document.getBidderCount(),
                document.getLocation(),
                document.getThumbnailUrl(),
                new SellerDto(document.getSellerId(), document.getSellerNickname(), null, null, null)
        );
    }
}
