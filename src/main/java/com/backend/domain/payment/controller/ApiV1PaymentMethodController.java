package com.backend.domain.payment.controller;


import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.dto.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.PaymentMethodResponse;
import com.backend.domain.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "PaymentMethod", description = "결제 수단 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/paymentMethods")
public class ApiV1PaymentMethodController {

    private final MemberRepository memberRepository;
    private final PaymentMethodService paymentMethodService;

    // 결제 수단 등록
    @Operation(summary = "결제 수단 등록", description = "카드, 계좌 등록")
    @PostMapping
    public PaymentMethodResponse create(
            @AuthenticationPrincipal User user,
            @RequestBody PaymentMethodCreateRequest request
    ) {
        // 인증 안 된 경우 방어(토큰 없음/유효하지 않음)..
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다. (Bearer 토큰을 Authorize에 입력하세요)");
        }

        String email = user.getUsername();

        // 이메일로 실제 회원 조회 → memberId 획득..
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다: " + email));
        Long memberId = member.getId();

        return paymentMethodService.create(memberId, request);
    }

    // 결제 수단 다건 조회..
    @GetMapping
    @Operation(summary = "결제 수단 다건 조회", description = "로그인한 사용자의 결제 수단 목록을 반환합니다.")
    public List<PaymentMethodResponse> list(@AuthenticationPrincipal User user) {
        // 인증 토큰 없으면 401
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        // Security의 username = email
        String email = user.getUsername();

        // 이메일로 Member 조회 → memberId 획득
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        Long memberId = member.getId();

        // 전체 조회 반환
        return paymentMethodService.findAll(memberId);
    }

    // 결제 수단 단건 조회..
    @GetMapping("/{id}")
    @Operation(summary = "결제 수단 단건 조회", description = "로그인한 사용자의 결제 수단 단건을 반환합니다.")
    public PaymentMethodResponse getOne(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String email = user.getUsername();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        Long memberId = member.getId();

        return paymentMethodService.findOne(memberId, paymentMethodId);
    }
}
