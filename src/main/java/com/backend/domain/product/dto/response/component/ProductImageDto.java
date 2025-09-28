package com.backend.domain.product.dto.response.component;

import com.backend.domain.product.entity.ProductImage;
import jakarta.validation.constraints.NotNull;

public record ProductImageDto(
        @NotNull Long id,
        @NotNull Long productId,
        @NotNull String imageUrl
) {
    public static ProductImageDto fromEntity(ProductImage entity) {
        return new ProductImageDto(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getImageUrl()
        );
    }
}
