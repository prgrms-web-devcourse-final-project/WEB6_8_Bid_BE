package com.backend.domain.cash.controller;

import com.backend.domain.cash.dto.CashResponse;
import com.backend.domain.cash.dto.CashTransactionResponse;
import com.backend.domain.cash.dto.CashTransactionsResponse;
import com.backend.domain.cash.service.CashService;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.member.service.MemberService;
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
@RequestMapping("/api/v1")
@Tag(name = "Cash", description = "돈 관련 API")
public class ApiV1CashController {

    private final MemberService memberService;
    private final CashService cashService;

    @GetMapping("/cash")
    @Operation(summary = "내 지갑 잔액 조회", description = "지갑이 없으면 404를 반환합니다.")
    @Transactional(readOnly = true)
    public CashResponse getMyCash(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Member actor = memberService.findMemberByEmail(user.getUsername());
        return cashService.getMyCashResponse(actor); // 컨트롤러는 위임만
    }

    @GetMapping("/cash/transactions")
    @Operation(summary = "내 원장 목록(입금/출금)", description = "지갑 미생성 시 404 반환, 페이지네이션 추가")
    @Transactional(readOnly = true)
    public CashTransactionsResponse getMyTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Member actor = memberService.findMemberByEmail(user.getUsername());
        return cashService.getMyTransactions(actor, page, size);
    }

    // 단건 상세..
    @GetMapping("/cash/transactions/{transactionId}")
    @Operation(summary = "내 원장 단건 상세")
    @Transactional(readOnly = true)
    public CashTransactionResponse getMyTransactionDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long transactionId
    ) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        Member actor = memberService.findMemberByEmail(user.getUsername());
        return cashService.getMyTransactionDetail(actor, transactionId);
    }
}
