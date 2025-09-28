package com.backend.domain.product.dto.response;

import com.backend.domain.product.dto.response.component.SellerDto;
import com.backend.domain.product.entity.Product;
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
}
