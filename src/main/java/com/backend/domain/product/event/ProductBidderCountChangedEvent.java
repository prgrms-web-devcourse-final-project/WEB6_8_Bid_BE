package com.backend.domain.product.event;

public record ProductBidderCountChangedEvent(
        Long productId,
        Integer oldBidderCount,
        Integer newBidderCount
) {}