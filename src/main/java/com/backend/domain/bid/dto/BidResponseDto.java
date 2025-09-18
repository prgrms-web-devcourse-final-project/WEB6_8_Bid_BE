package com.backend.domain.bid.dto;

import java.time.LocalDateTime;

public record BidResponseDto (
        Long id,
        Long productId,
        Long bidderId,
        long price,
        String status,
        LocalDateTime createDate
) {}
