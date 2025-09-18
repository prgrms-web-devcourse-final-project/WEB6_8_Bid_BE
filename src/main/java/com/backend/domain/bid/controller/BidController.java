package com.backend.domain.bid.controller;

import com.backend.domain.bid.dto.BidCurrentResponseDto;
import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.dto.BidResponseDto;
import com.backend.domain.bid.service.BidService;
import com.backend.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/products/{productId}")
    public RsData<BidResponseDto> createBid(
            @PathVariable Long productId,
            @Valid @RequestBody BidRequestDto request) {

        // TODO: JWT 토큰에서 사용자 추출
        Long bidderId = 1L;

        return bidService.createBid(productId,bidderId,request);
    }

    @GetMapping("/products/{productId}")
    public RsData<BidCurrentResponseDto>  getBidStatus(
            @PathVariable Long productId
    ){
        return bidService.getBidStatus(productId);
    }

}
