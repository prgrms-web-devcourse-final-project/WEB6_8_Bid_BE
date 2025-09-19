package com.backend.domain.product.dto;

public record ProductSearchDto(
    String keyword,
    Integer[] category,
    String[] location,
    Boolean isDelivery
) {}
