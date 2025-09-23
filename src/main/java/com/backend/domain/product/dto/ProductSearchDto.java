package com.backend.domain.product.dto;

import com.backend.domain.product.enums.AuctionStatus;

public record ProductSearchDto(
    String keyword,
    Integer[] category,
    String[] location,
    Boolean isDelivery,
    AuctionStatus status
) {}
