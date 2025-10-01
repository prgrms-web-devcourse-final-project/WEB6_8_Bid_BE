package com.backend.domain.bid.dto;

import java.time.LocalDateTime;

public record BidPayResponseDto(
        Long bidId,
        Long productId,
        Long amount,
        LocalDateTime paidAt,
        Long cashTransactionId,
        Long balanceAfter
) {}