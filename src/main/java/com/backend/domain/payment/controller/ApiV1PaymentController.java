package com.backend.domain.payment.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.payment.dto.PaymentRequest;
import com.backend.domain.payment.dto.PaymentResponse;
import com.backend.domain.payment.dto.TossIssueBillingKeyRequest;
import com.backend.domain.payment.dto.TossIssueBillingKeyResponse;
import com.backend.domain.payment.service.PaymentService;
import com.backend.domain.payment.service.TossBillingClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "지갑 충전 API")
public class ApiV1PaymentController {

    private final MemberService memberService;
    private final PaymentService paymentService;
    private final TossBillingClient tossBillingClient;

    @PostMapping
    @Operation(summary="지갑 충전 요청", description="idempotencyKey로 중복 충전 방지, 일단은 idempotencyKey 아무키로 등록해주세요!.")
    public PaymentResponse charge(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid PaymentRequest req
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Member actor = memberService.findMemberByEmail(user.getUsername());

        return paymentService.charge(actor, req);
    }

    @PostMapping("/toss/issue-billing-key")
    public TossIssueBillingKeyResponse issueBillingKey(
            @AuthenticationPrincipal User user,
            @RequestBody TossIssueBillingKeyRequest req
    ) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Member me = memberService.findMemberByEmail(user.getUsername());
        String customerKey = "user-" + me.getId();   // ★ 프런트 값 무시하고 서버에서 생성

        return tossBillingClient.issueBillingKey(customerKey, req.getAuthKey());
    }
}
