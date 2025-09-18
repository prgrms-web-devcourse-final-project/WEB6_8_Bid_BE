package com.backend.domain.bid.dto;

import java.time.LocalDateTime;

public record BidResponseDto (
  Integer id,
  Integer productId,
  Integer bidderId,
  long price,
  String status,
  LocalDateTime createDate
) {}
