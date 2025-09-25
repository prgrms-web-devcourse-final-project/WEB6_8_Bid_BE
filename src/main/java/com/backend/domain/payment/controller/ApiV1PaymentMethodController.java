package com.backend.domain.payment.controller;


import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.dto.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.PaymentMethodResponse;
import com.backend.domain.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
