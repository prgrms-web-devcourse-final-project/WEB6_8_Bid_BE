package com.backend.domain.payment.controller;


import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.payment.dto.request.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.response.PaymentMethodDeleteResponse;
import com.backend.domain.payment.dto.request.PaymentMethodEditRequest;
import com.backend.domain.payment.dto.response.PaymentMethodResponse;
import com.backend.domain.payment.service.PaymentMethodService;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "PaymentMethod", description = "결제 수단 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/paymentMethods")
public class ApiV1PaymentMethodController {

    private final MemberService memberService;
    private final PaymentMethodService paymentMethodService;

    // 공통: 인증 사용자(Member) 가져오기..
    private Member getActor(User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return memberService.findMemberByEmail(user.getUsername());
    }

    @PostMapping
    @Operation(summary = "결제 수단 등록", description = "type: card, bank \n\n" + "CARD 등록: alias, isDefault, brand, last4, expMonth, expYear만 보내고 bankCode, bankName, acctLast4는 넣지마세요!\n\n" +
            "BANK 등록: alias, isDefault, bankCode(선택), bankName, acctLast4만 보내고 brand, last4, expMonth, expYear는 넣지마세요!")
    public RsData<PaymentMethodResponse> create(
            @AuthenticationPrincipal User user,
            @RequestBody PaymentMethodCreateRequest request
    ) {
        Member actor = getActor(user);
        PaymentMethodResponse data = paymentMethodService.create(actor.getId(), request);

        return RsData.created("결제수단이 등록되었습니다.", data);
    }

    @GetMapping
    @Operation(summary = "결제 수단 다건 조회", description = "로그인한 사용자의 결제 수단 목록을 반환합니다.")
    @Transactional(readOnly = true)
    public RsData<List<PaymentMethodResponse>> list(@AuthenticationPrincipal User user) {
        Member actor = getActor(user);
        List<PaymentMethodResponse> data = paymentMethodService.findAll(actor.getId());

        return RsData.ok("결제수단 목록이 조회되었습니다.", data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "결제 수단 단건 조회", description = "로그인한 사용자의 결제 수단 단건을 반환합니다.")
    public RsData<PaymentMethodResponse> getOne(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId
    ) {
        Member actor = getActor(user);
        PaymentMethodResponse data = paymentMethodService.findOne(actor.getId(), paymentMethodId);

        return RsData.ok("결제 수단이 등록되었습니다.", data);
    }

    @PutMapping("/{id}")
    @Operation(summary = "결제 수단 수정", description = "CARD 수정: alias, isDefault, brand, last4, expMonth, expYear만 보내고 bankCode, bankName, acctLast4는 삭제\n\n" +
            "BANK 수정: alias, isDefault, bankCode(선택), bankName, acctLast4만 보내고 brand, last4, expMonth, expYear는 삭제")
    public RsData<PaymentMethodResponse> edit(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId,
            @RequestBody PaymentMethodEditRequest request
    ) {
        Member actor = getActor(user);
        PaymentMethodResponse data = paymentMethodService.edit(actor.getId(), paymentMethodId, request);

        return RsData.ok("결제수단이 수정되었습니다.", data);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "결제 수단 삭제", description = "기본 수단 삭제 시 최근 생성 수단으로 자동 승계합니다.")
    public RsData<PaymentMethodDeleteResponse> delete(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId
    ) {
        Member actor = getActor(user);
        PaymentMethodDeleteResponse data = paymentMethodService.deleteAndReport(actor.getId(), paymentMethodId);

        return RsData.ok("결제수단이 삭제되었습니다.", data);
    }


}
