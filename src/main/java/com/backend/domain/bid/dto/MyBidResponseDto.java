package com.backend.domain.bid.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyBidResponseDto (
        List<MyBidItem> content,
        Integer totalElements,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize,
        Boolean hasNext
){
    public record MyBidItem (
            Long bidId,
            Long productId,
            String productName,
            String thumbnailUrl,
            Long myBidPrice,
            Long currentPrice,
            String status,
            Boolean isWinning,
            LocalDateTime bidTime,
            LocalDateTime endTime,
            String productStatus,
            SellerInfo Seller
    ) {}
    public record SellerInfo (
            Integer id,
            String nickname
    ) {}
}
