package com.backend.domain.bid.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BidCurrentResponseDto (
        Long productId,
        String productName,
        Long currentPrice,
        Long initialPrice,
        Long bidCount,
        String status,
        LocalDateTime endTime,
        String remainingTime,
        List<RecentBid> recentBids
        ){
    public record RecentBid (
            Long bidPrice,
            LocalDateTime bidTime,
            String bidder // 익명화
    ){}
}
