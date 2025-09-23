package com.backend.domain.bid.controller;

import com.backend.domain.bid.dto.BidCurrentResponseDto;
import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.dto.BidResponseDto;
import com.backend.domain.bid.dto.MyBidResponseDto;
import com.backend.domain.bid.service.BidService;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class ApiV1BidController {

    private final BidService bidService;
    private final MemberRepository memberRepository;

    @PostMapping("/products/{productId}")
    public RsData<BidResponseDto> createBid(
            @PathVariable Long productId,
            @Valid @RequestBody BidRequestDto request,
            @AuthenticationPrincipal User user) {
        // TODO: JWT 토큰에서 사용자 추출로직으로 대체
        Long bidderId = Long.parseLong(user.getUsername());
        return bidService.createBid(productId, bidderId, request);
    }

    @GetMapping("/products/{productId}")
    public RsData<BidCurrentResponseDto>  getBidStatus(
            @PathVariable Long productId
    ){
        return bidService.getBidStatus(productId);
    }

    @GetMapping("/me")
    public RsData<MyBidResponseDto> getMyBids(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user
    ){
        // TODO: JWT 토큰에서 사용자 추출로직으로 대체
        Long memberId = Long.parseLong(user.getUsername());
        return bidService.getMyBids(memberId,page,size);
    }


}
