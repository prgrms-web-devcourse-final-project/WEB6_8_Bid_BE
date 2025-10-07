package com.backend.domain.cash.controller;

import com.backend.domain.cash.dto.response.CashResponse;
import com.backend.domain.cash.dto.response.CashTransactionResponse;
import com.backend.domain.cash.dto.response.CashTransactionsResponse;
import com.backend.domain.cash.service.CashService;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cash")
@Tag(name = "Cash", description = "돈 관련 API")
public class ApiV1CashController {

    private final MemberService memberService;
    private final CashService cashService;

    @GetMapping
    @Operation(summary = "내 지갑 잔액 조회", description = "지갑이 없으면 404를 반환합니다.")
    @Transactional(readOnly = true)
    public RsData<CashResponse> getMyCash(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Member actor = memberService.findMemberByEmail(user.getUsername());
        CashResponse data = cashService.getMyCashResponse(actor);
        return RsData.ok("지갑 잔액이 조회되었습니다.", data);
    }

    @GetMapping("/transactions")
    @Operation(summary = "내 원장 목록(입금/출금)", description = "지갑 미생성 시 404 반환")
    @Transactional(readOnly = true)
    public RsData<CashTransactionsResponse> getMyTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Member actor = memberService.findMemberByEmail(user.getUsername());
        CashTransactionsResponse data = cashService.getMyTransactions(actor, page, size);
        return RsData.ok("지갑 원장 목록이 조회되었습니다.", data);
    }

    @GetMapping("/transactions/{transactionId}")
    @Operation(summary = "내 원장 단건 상세")
    @Transactional(readOnly = true)
    public RsData<CashTransactionResponse> getMyTransactionDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long transactionId
    ) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        Member actor = memberService.findMemberByEmail(user.getUsername());
        CashTransactionResponse data = cashService.getMyTransactionDetail(actor, transactionId);
        return RsData.ok("지갑 원장 상세가 조회되었습니다.", data);
    }
}
