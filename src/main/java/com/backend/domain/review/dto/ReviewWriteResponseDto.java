package com.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ReviewWriteResponseDto(
        Long reviewerId,
        String reviewerName,
        String ProductName,
        String comment,
        Boolean isSatisfied
) {
}
