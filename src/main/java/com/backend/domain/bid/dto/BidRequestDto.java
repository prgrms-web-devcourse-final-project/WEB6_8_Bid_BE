package com.backend.domain.bid.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BidRequestDto(
        @NotNull(message = "입찰 금액은 필수입니다.")
        @Positive(message = "입찰 금액은 0보다 커야 합니다.")
        Long price
) {}