package com.backend.domain.review.dto;

public record ReviewEditResponseDto(
        Long reviewerId,
        String reviewerName,
        String ProductName,
        String comment,
        Boolean isSatisfied
) {
}
