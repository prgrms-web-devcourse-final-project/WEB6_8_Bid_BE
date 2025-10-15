package com.backend.domain.payment.controller;


import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.payment.dto.request.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.response.PaymentMethodDeleteResponse;
import com.backend.domain.payment.dto.request.PaymentMethodEditRequest;
import com.backend.domain.payment.dto.response.PaymentMethodResponse;
import com.backend.domain.payment.dto.response.TossConfirmResultResponse;
import com.backend.domain.payment.dto.response.TossIssueBillingKeyResponse;
import com.backend.domain.payment.service.PaymentMethodService;
import com.backend.domain.payment.service.TossBillingClientService;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final TossBillingClientService tossBillingClientService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    // 공통: 인증 사용자(Member) 가져오기..
    private Member getActor(User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return memberService.findMemberByEmail(user.getUsername());
    }

    @PostMapping
    @Operation(summary = "결제 수단 등록", description = "type: card, bank \n\n" + "CARD 등록: alias, isDefault, brand, last4, expMonth, expYear만 보내고 bankCode, bankName, acctLast4는 넣지마세요!\n\n" +
            "BANK 등록: alias, isDefault, bankCode(선택), bankName, acctLast4만 보내고 brand, last4, expMonth, expYear는 넣지마세요!")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "409", description = "별명(alias) 중복 등 충돌",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<PaymentMethodResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @RequestBody PaymentMethodCreateRequest request
    ) {

        Member actor = getActor(user);
        PaymentMethodResponse data = paymentMethodService.create(actor.getId(), request);

        return RsData.created("결제수단이 등록되었습니다.", data);
    }

    @GetMapping
    @Operation(summary = "결제 수단 다건 조회", description = "로그인한 사용자의 결제 수단 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @Transactional(readOnly = true)
    public RsData<List<PaymentMethodResponse>> list(@AuthenticationPrincipal User user) {
        Member actor = getActor(user);
        List<PaymentMethodResponse> data = paymentMethodService.findAll(actor.getId());

        return RsData.ok("결제수단 목록이 조회되었습니다.", data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "결제 수단 단건 조회", description = "로그인한 사용자의 결제 수단 단건을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "단건 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "결제 수단 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<PaymentMethodResponse> getOne(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId
    ) {
        Member actor = getActor(user);
        PaymentMethodResponse data = paymentMethodService.findOne(actor.getId(), paymentMethodId);

        return RsData.ok("결제 수단이 등록되었습니다.", data);
    }

    @PutMapping("/{id}")
    @Operation(summary = "결제 수단 수정", description = "수정 시에는 alias, isDefault만 반영합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "결제수단 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "409", description = "별명(alias) 중복 등 충돌",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<PaymentMethodResponse> edit(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId,
            @RequestBody PaymentMethodEditRequest request
    ) {
        Member actor = getActor(user);
        PaymentMethodResponse data = paymentMethodService.edit(actor.getId(), paymentMethodId, request);

        return RsData.ok("결제수단이 수정되었습니다.", data);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "결제 수단 삭제", description = "기본 수단 삭제 시 최근 생성 수단으로 자동 승계합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "결제수단 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    public RsData<PaymentMethodDeleteResponse> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PathVariable("id") Long paymentMethodId
    ) {
        Member actor = getActor(user);
        PaymentMethodDeleteResponse data = paymentMethodService.deleteAndReport(actor.getId(), paymentMethodId);

        return RsData.ok("결제수단이 삭제되었습니다.", data);
    }

    // Toss success/fail 리다이렉트가 도달하는 콜백
    @GetMapping("/toss/confirm-callback")
    public ResponseEntity<Void> confirmCallback(
            @RequestParam(required = false) String customerKey,
            @RequestParam(required = false) String authKey,
            @RequestParam(required = false, defaultValue = "") String result
    ) {
        try {
            if (!"success".equalsIgnoreCase(result)) {
                // 실패: FE로 실패 리다이렉트
                String location = frontendBaseUrl + "/wallet?billing=fail";
                return ResponseEntity.status(302).header("Location", location).build();
            }

            // 파라미터 체크
            if (customerKey == null || authKey == null) {
                String location = frontendBaseUrl + "/wallet?billing=fail&reason=missing_param";
                return ResponseEntity.status(302).header("Location", location).build();
            }

            // 1) authKey → billingKey 교환(Confirm)
            TossIssueBillingKeyResponse confirm = tossBillingClientService.issueBillingKey(customerKey, authKey);

            // 2) customerKey("user-123")에서 회원 ID 추출
            Long memberId = parseMemberIdFromCustomerKey(customerKey);

            // 3) 결제수단 저장/업데이트 (brand/last4 등 스냅샷 포함)
            paymentMethodService.saveOrUpdateBillingKey(memberId, confirm);

            // 4) 성공 → FE /wallet 으로 리다이렉트
            String location = frontendBaseUrl + "/wallet";
            return ResponseEntity.status(302).header("Location", location).build();

        } catch (Exception e) {
            String location = frontendBaseUrl + "/wallet?billing=fail&reason=server_error";
            return ResponseEntity.status(302).header("Location", location).build();
        }
    }

    private Long parseMemberIdFromCustomerKey(String customerKey) {
        if (customerKey != null && customerKey.startsWith("user-")) {
            return Long.parseLong(customerKey.substring("user-".length()));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid customerKey");
    }
}
