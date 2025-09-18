package com.backend.domain.bid.dto.response;

import java.time.LocalDateTime;

public record BidData(
    Integer id,
    Integer productId,
    Integer bidderId,
    Long price,
    String status,
    LocalDateTime createDate
) {}
