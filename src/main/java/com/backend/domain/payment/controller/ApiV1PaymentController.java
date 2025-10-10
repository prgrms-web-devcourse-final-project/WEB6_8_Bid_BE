package com.backend.domain.payment.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.payment.dto.request.PaymentRequest;
import com.backend.domain.payment.dto.request.TossIssueBillingKeyRequest;
import com.backend.domain.payment.dto.response.MyPaymentResponse;
import com.backend.domain.payment.dto.response.MyPaymentsResponse;
import com.backend.domain.payment.dto.response.PaymentResponse;
import com.backend.domain.payment.dto.response.TossIssueBillingKeyResponse;
import com.backend.domain.payment.service.PaymentService;
import com.backend.domain.payment.service.TossBillingClientService;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "지갑 충전 API")
public class ApiV1PaymentController {

    private final MemberService memberService;
    private final PaymentService paymentService;
    private final TossBillingClientService tossBillingClientService;

    // 공통: 인증 사용자(Member) 가져오기..
    private Member getActor(User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return memberService.findMemberByEmail(user.getUsername());
    }

    @PostMapping
    @Operation(summary="지갑 충전 요청", description="idempotencyKey로 중복 충전 방지, 일단은 idempotencyKey 아무키로 등록해주세요!")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "충전 완료",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "409", description = "멱등키 충돌",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<PaymentResponse> charge(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @RequestBody @Valid PaymentRequest req
    ) {
        Member actor = getActor(user);
        PaymentResponse data = paymentService.charge(actor, req);
        return RsData.created("지갑 충전이 완료되었습니다.", data);
    }

    @PostMapping("/toss/issue-billing-key")
    @Operation(summary="토스 빌링키 발급", description="카드/계좌 등록용 빌링키를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "502", description = "PG 연동 오류",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<TossIssueBillingKeyResponse> issueBillingKey(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @RequestBody TossIssueBillingKeyRequest req
    ) {
        Member me = getActor(user);
        String customerKey = "user-" + me.getId();
        TossIssueBillingKeyResponse data = tossBillingClientService.issueBillingKey(customerKey, req.getAuthKey());

        return RsData.ok("빌링키가 발급되었습니다.", data);
    }

    @GetMapping("/me")
    @Operation(summary="내 결제 내역")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내역 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @Transactional(readOnly = true)
    public RsData<MyPaymentsResponse> getMyPayments(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Member actor = getActor(user);
        MyPaymentsResponse data = paymentService.getMyPayments(actor, page, size);

        return RsData.ok("내 결제 내역이 조회되었습니다.", data);
    }

    @GetMapping("/me/{paymentId}")
    @Operation(summary = "내 결제 단건 상세")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "결제 내역 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @Transactional(readOnly = true)
    public RsData<MyPaymentResponse> getMyPaymentDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PathVariable Long paymentId
    ) {
        Member actor =  getActor(user);
        MyPaymentResponse data = paymentService.getMyPaymentDetail(actor, paymentId);

        return RsData.ok("결제 상세가 조회되었습니다.", data);
    }

}
