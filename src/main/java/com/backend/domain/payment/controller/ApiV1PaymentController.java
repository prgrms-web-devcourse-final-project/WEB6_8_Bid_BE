package com.backend.domain.payment.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.payment.dto.request.PaymentRequest;
import com.backend.domain.payment.dto.request.TossIssueBillingKeyRequest;
import com.backend.domain.payment.dto.response.*;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "지갑 충전 API")
public class ApiV1PaymentController {

    @Value("${pg.toss.clientKey}")
    private String tossClientKey;

    private final MemberService memberService;
    private final PaymentService paymentService;
    private final TossBillingClientService tossBillingClientService;

    // 공통: 인증 사용자(Member) 가져오기..
    private Member getActor(User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return memberService.findMemberByEmail(user.getUsername());
    }

    @PostMapping
    @Operation(summary="지갑 충전 요청", description="idempotencyKey로 중복 충전 방지")
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

    @GetMapping("/toss/billing-auth-params")
    @Operation(
            summary = "토스 카드등록(빌링) 팝업 파라미터 조회",
            description = """
            토스 카드 등록 창을 띄우기 위해 FE가 먼저 호출하는 엔드포인트입니다.
            - 로그인(인증) 필요: 서버가 로그인 사용자의 customerKey(`user-{id}`)를 만들어 줍니다.
            - 응답 값:
              * clientKey   : Toss Payments 공개 키 (FE에서 Toss SDK 초기화에 사용)
              * customerKey : 사용자 고유키 (카드 등록/결제 시 동일 값 사용)
              * successUrl  : 카드 등록 성공 후 리다이렉트될 페이지
              * failUrl     : 카드 등록 실패 시 리다이렉트될 페이지
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<TossBillingAuthParamsResponse> getBillingAuthParams(
            @AuthenticationPrincipal User user, HttpServletRequest req) {

        Member me = memberService.findMemberByEmail(user.getUsername());
        String customerKey = "user-" + me.getId();

        // origin 계산(예: http://localhost:8080)
        String origin = req.getScheme() + "://" + req.getServerName()
                + ((req.getServerPort()==80 || req.getServerPort()==443) ? "" : ":" + req.getServerPort());

        TossBillingAuthParamsResponse data = new TossBillingAuthParamsResponse(
                tossClientKey,
                customerKey,
                origin + "/payments/toss/billing-success.html",
                origin + "/payments/toss/billing-fail.html"
        );
        return RsData.ok("ok", data);
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

    @GetMapping("/idempotency-key")
    @Operation(
            summary = "멱등키(결제 재시도 식별 키) 발급",
            description = "결제 요청 전에 1회 호출해서 받은 키를 재시도 시에도 동일하게 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<IdempotencyKeyResponse> newIdempotencyKey() {
        String key = UUID.randomUUID().toString();
        return RsData.ok("멱등키가 발급되었습니다.", new IdempotencyKeyResponse(key));
    }

}
