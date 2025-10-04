package com.backend.domain.bid.dto;

import com.backend.domain.bid.enums.BidStatus;

import java.time.LocalDateTime;

public record BidResponseDto(
        Long id,
        Long productId,
        Long bidderId,
        long price,
        BidStatus status,
        LocalDateTime createDate
) {
}
