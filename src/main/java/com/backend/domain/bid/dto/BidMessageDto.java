package com.backend.domain.bid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidMessageDto {
    private Long productId;
    private Long bidderId;
    private Long price;
}
