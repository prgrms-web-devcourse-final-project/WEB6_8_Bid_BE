package com.backend.domain.product.event;

public record ProductStatusChangedEvent(
        Long productId,
        String oldStatus,
        String newStatus
) {
}