package com.backend.domain.product.event;

public record ProductPriceChangedEvent(
        Long productId,
        Long oldPrice,
        Long newPrice
) {}