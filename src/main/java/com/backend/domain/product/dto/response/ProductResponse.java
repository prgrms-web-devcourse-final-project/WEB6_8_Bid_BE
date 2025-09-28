package com.backend.domain.product.dto.response;

import com.backend.domain.product.dto.response.component.ProductImageDto;
import com.backend.domain.product.dto.response.component.SellerDto;
import com.backend.domain.product.entity.Product;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        @NotNull Long productId,
        @NotNull String name,
        String description,
        @NotNull String category,
        @NotNull Long initialPrice,
        @NotNull Long currentPrice,
        @NotNull LocalDateTime auctionStartTime,
        @NotNull LocalDateTime auctionEndTime,
        @NotNull Integer auctionDuration,
        @NotNull String status,
//        @NotNull Long biddersCount,
        @NotNull String deliveryMethod,
        String location,
        @NotNull List<ProductImageDto> images,
        @NotNull SellerDto seller,
        @NotNull LocalDateTime createDate,
        @NotNull LocalDateTime modifyDate
) {
    public static ProductResponse fromEntity(Product entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getProductName(),
                entity.getDescription(),
                entity.getCategory().getDisplayName(),
                entity.getInitialPrice(),
                entity.getCurrentPrice(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDuration(),
                entity.getStatus(),
//                entity.getBiddersCount(),
                entity.getDeliveryMethod().name(),
                entity.getLocation(),
                entity.getProductImages().stream().map(ProductImageDto::fromEntity).toList(),
                SellerDto.fromEntity(entity.getSeller()),
                entity.getCreateDate(),
                entity.getModifyDate()
        );
    }
}
