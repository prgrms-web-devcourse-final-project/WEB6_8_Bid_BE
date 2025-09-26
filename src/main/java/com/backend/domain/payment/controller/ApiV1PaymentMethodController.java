package com.backend.domain.payment.controller;


import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.dto.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.PaymentMethodDeleteResponse;
import com.backend.domain.payment.dto.PaymentMethodEditRequest;
import com.backend.domain.payment.dto.PaymentMethodResponse;
import com.backend.domain.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "결제 수단 등록", description = "type: card, bank \n\n" + "CARD 등록: alias, isDefault, brand, last4, expMonth, expYear만 보내고 bankCode, bankName, acctLast4는 넣지마세요!\n\n" +
            "BANK 등록: alias, isDefault, bankCode(선택), bankName, acctLast4만 보내고 brand, last4, expMonth, expYear는 넣지마세요!")
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

    @PutMapping("/{id}")
    @Operation(summary = "결제 수단 수정", description = "CARD 수정: alias, isDefault, brand, last4, expMonth, expYear만 보내고 bankCode, bankName, acctLast4는 삭제\n\n" +
            "BANK 수정: alias, isDefault, bankCode(선택), bankName, acctLast4만 보내고 brand, last4, expMonth, expYear는 삭제")
    public PaymentMethodResponse edit(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId,
            @RequestBody PaymentMethodEditRequest request
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String email = user.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        return paymentMethodService.edit(member.getId(), paymentMethodId, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "결제 수단 삭제", description = "기본 수단 삭제 시 최근 생성 수단으로 자동 승계합니다.")
    public ResponseEntity<PaymentMethodDeleteResponse> delete(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId
    ) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        Member member = memberRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        PaymentMethodDeleteResponse result = paymentMethodService.deleteAndReport(member.getId(), paymentMethodId);
        return ResponseEntity.ok(result); // 200 + 바디
    }


}
