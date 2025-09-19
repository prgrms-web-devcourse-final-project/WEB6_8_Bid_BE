package com.backend.domain.product.dto;

import com.backend.domain.product.enums.Location;

public record ProductSearchDto(
    String keyword,
    Integer category,
    Location[] location,
    Boolean isDelivery
) {}
