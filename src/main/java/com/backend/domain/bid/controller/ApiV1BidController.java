package com.backend.domain.bid.controller;

import com.backend.domain.bid.dto.*;
import com.backend.domain.bid.service.BidService;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Bid", description = "입찰 관련 API")
@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class ApiV1BidController {

    private final BidService bidService;
    private final MemberRepository memberRepository;

    @Operation(summary = "입찰 생성", description = "특정 상품에 대해 입찰 생성.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "입찰 생성 성공",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @PostMapping("/products/{productId}")
    public RsData<BidResponseDto> createBid(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId,
            @Parameter(description = "입찰 요청 정보", required = true) @Valid @RequestBody BidRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        // TODO: JWT 토큰에서 사용자 추출로직으로 대체
        Long bidderId;
        if (user != null) {
            bidderId = Long.parseLong(user.getUsername());
        } else {
            // 테스트용: 인증이 없으면 첫 번째 사용자 사용
            bidderId = 1L;
        }
        return bidService.createBid(productId, bidderId, request);
    }

    @Operation(summary = "입찰 현황 조회", description = "특정 상품의 현재 입찰 현황 조회.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "입찰 현황 조회 성공",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @GetMapping("/products/{productId}")
    public RsData<BidCurrentResponseDto> getBidStatus(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId
    ){
        return bidService.getBidStatus(productId);
    }

    @Operation(summary = "내 입찰 내역 조회", description = "현재 사용자의 입찰 내역을 페이지네이션으로 조회.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "내 입찰 내역 조회 성공",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @GetMapping("/me")
    public RsData<MyBidResponseDto> getMyBids(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ){
        // TODO: JWT 토큰에서 사용자 추출로직으로 대체
        Long memberId = Long.parseLong(user.getUsername());
        return bidService.getMyBids(memberId,page,size);
    }

    @Operation(summary = "낙찰 결제", description = "내가 낙찰한 입찰 건에 대해 지갑에서 출금하고 결제 완료로 표시합니다.")
    @PostMapping("/{bidId}/pay")
    public RsData<BidPayResponseDto> payBid(
            @PathVariable Long bidId,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Long memberId;
        String username = user.getUsername();
        try {
            memberId = Long.parseLong(username);
        } catch (NumberFormatException e) {
            var me = memberRepository.findByEmail(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
            memberId = me.getId();
        }

        return bidService.payForBid(memberId, bidId);
    }
}
