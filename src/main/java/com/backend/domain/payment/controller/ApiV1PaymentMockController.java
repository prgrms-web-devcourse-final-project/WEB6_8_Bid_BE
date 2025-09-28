package com.backend.domain.payment.controller;


import com.backend.domain.payment.dto.*;
import com.backend.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment", description = "결제 관련 API")
@RestController
@RequestMapping("/api/v1/payments")
public class ApiV1PaymentMockController {

    @Operation(summary = "결제 수단 다건 조회", description = "카드, 계좌 다건 조회")
    @GetMapping("/paymentMethods")
    public ResponseEntity<RsData<List<PaymentMethodResponse>>> getPaymentMethods() {

        // 카드..
        PaymentMethodResponse card = PaymentMethodResponse.builder()
                .id(12L)
                .type("CARD")
                .alias("결혼식 카드")
                .isDefault(true)
                .brand("SHINHAN")
                .last4("1234")
                .expMonth(12)
                .expYear(2027)
                .bankCode(null)
                .bankName(null)
                .acctLast4(null)
                .createDate("2025-09-23T12:34:56Z")
                .modifyDate("2025-09-23T12:34:56Z")
                .expireDate("2027-12")
                .build();

        // 계좌..
        PaymentMethodResponse bank = PaymentMethodResponse.builder()
                .id(21L)
                .type("BANK")
                .alias("급여통장")
                .isDefault(false)
                .brand(null)
                .last4(null)
                .expMonth(null)
                .expYear(null)
                .bankCode("004")
                .bankName("KB국민은행")
                .acctLast4("5678")
                .createDate("2025-08-11T09:10:00Z")
                .modifyDate("2025-08-11T09:10:00Z")
                .expireDate(null)
                .build();

        List<PaymentMethodResponse> data = List.of(card, bank);
        RsData<List<PaymentMethodResponse>> body =
                new RsData<>("200", "결제수단 목록이 조회되었습니다.", data);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/paymentMethods/{id}")
    @Operation(summary = "결제 수단 단건 조회", description = "카드 또는 계좌 단건 조회")
    public ResponseEntity<RsData<PaymentMethodResponse>> getPaymentMethod(@PathVariable Long id) {

        // 예시 규칙: id가 12면 카드, 아니면 계좌..
        PaymentMethodResponse data;

        if (id != null && id == 12L) {

            // 카드..
            data = PaymentMethodResponse.builder()
                    .id(12L)
                    .type("CARD")
                    .alias("결혼식 카드")
                    .isDefault(true)
                    .brand("SHINHAN")
                    .last4("1234")
                    .expMonth(12)
                    .expYear(2027)
                    .bankCode(null)
                    .bankName(null)
                    .acctLast4(null)
                    .createDate("2025-09-23T12:34:56Z")
                    .modifyDate("2025-09-23T12:34:56Z")
                    .expireDate("2027-12")
                    .build();
        } else {

            // 계좌..
            data = PaymentMethodResponse.builder()
                    .id(id)
                    .type("BANK")
                    .alias("급여통장")
                    .isDefault(false)
                    .brand(null)
                    .last4(null)
                    .expMonth(null)
                    .expYear(null)
                    .bankCode("004")
                    .bankName("KB국민은행")
                    .acctLast4("5678")
                    .createDate("2025-08-11T09:10:00Z")
                    .modifyDate("2025-08-11T09:10:00Z")
                    .expireDate(null)
                    .build();
        }

        RsData<PaymentMethodResponse> body =
                new RsData<>("200", "결제수단이 조회되었습니다.", data);

        return ResponseEntity.ok(body);
    }

    @PostMapping("/paymentMethods")
    @Operation(summary = "결제 수단 등록", description = "카드 또는 계좌 등록")
    public ResponseEntity<RsData<PaymentMethodResponse>> createPaymentMethod(
    ) {
        PaymentMethodResponse data = PaymentMethodResponse.builder()
                .id(34L)
                .type("CARD")
                .alias("주거래 카드")
                .isDefault(true)
                .brand("KB")
                .last4("4321")
                .expMonth(3)
                .expYear(2028)
                .bankCode(null)
                .bankName(null)
                .acctLast4(null)
                .createDate("2025-09-23T13:20:00Z")
                .modifyDate("2025-09-23T13:20:00Z")
                .expireDate("2028-03")
                .build();

        RsData<PaymentMethodResponse> body =
                new RsData<>("201", "결제수단이 등록되었습니다.", data);

        return ResponseEntity.ok(body);
    }

    @PutMapping("/paymentMethods/{id}")
    @Operation(summary = "결제 수단 수정")
    public ResponseEntity<RsData<PaymentMethodEditResponse>> editPaymentMethod(
            @PathVariable Long id
    ) {
        PaymentMethodEditResponse data = PaymentMethodEditResponse.builder()
                .id(id)
                .alias("경조사용 카드")
                .isDefault(false)
                .modifyDate("2025-09-23T13:45:00Z")
                .build();

        RsData<PaymentMethodEditResponse> body =
                new RsData<>("200", "결제수단이 수정되었습니다.", data);

        return ResponseEntity.ok(body); // HTTP 200 OK
    }

    @DeleteMapping("/paymentMethods/{id}")
    @Operation(summary = "결제 수단 삭제")
    public ResponseEntity<RsData<PaymentMethodDeleteResponse>> deletePaymentMethod(
            @PathVariable Long id
    ) {
        PaymentMethodDeleteResponse data = PaymentMethodDeleteResponse.builder()
                .id(id)
                .deleted(true)
                .build();

        RsData<PaymentMethodDeleteResponse> body =
                new RsData<>("200", "결제수단이 삭제되었습니다.", data);

        return ResponseEntity.ok(body); // HTTP 200 OK
    }

    @PostMapping("/payments")
    @Operation(summary = "결제 요청(충전)", description = "돈을 충전합니다.")
    public ResponseEntity<RsData<PaymentResponse>> charge() {

        PaymentResponse data = PaymentResponse.builder()
                .paymentId(101L)
                .paymentMethodId(12L)
                .status("SUCCESS")
                .amount(50000L)
                .currency("KRW")
                .provider("toss")
                .methodType("CARD")
                .transactionId("pg_tx_abc123")
                .createdAt("2025-09-23T12:35:10Z")
                .paidAt("2025-09-23T12:35:10Z")
                .idempotencyKey("topup-20250923-uid123-001")
                .cashTransactionId(98765L)
                .balanceAfter(155000L)
                .build();

        RsData<PaymentResponse> body =
                new RsData<>("200", "지갑 충전이 완료되었습니다.", data);

        return ResponseEntity.status(201).body(body);
    }

    @GetMapping("/payments/me")
    @Operation(summary = "내 결제 내역 조회")
    public ResponseEntity<RsData<MyPaymentsResponse>> getMyPayments(
            @RequestParam(defaultValue = "1") int page,   // 몇 번째 페이지인지..
            @RequestParam(defaultValue = "20") int size   // 한 페이지에 몇 개 보여줄지..
    ) {
        // 성공..
        MyPaymentListItemResponse item1 = MyPaymentListItemResponse.builder()
                .paymentId(101L)
                .paymentMethodId(12L)
                .status("SUCCESS")
                .amount(50000L)
                .currency("KRW")
                .provider("toss")
                .methodType("CARD")
                .transactionId("pg_tx_abc123")          // 성공이라 있음..
                .createdAt("2025-09-23T12:35:10Z")
                .build();

        // 실패..
        MyPaymentListItemResponse item2 = MyPaymentListItemResponse.builder()
                .paymentId(99L)
                .paymentMethodId(21L)
                .status("FAILED")
                .amount(30000L)
                .currency("KRW")
                .provider("toss")
                .methodType("CARD")
                .transactionId(null)                    // 실패라서 없음..
                .createdAt("2025-09-15T08:10:00Z")
                .build();

        List<MyPaymentListItemResponse> items = List.of(item1, item2);
        MyPaymentsResponse data = MyPaymentsResponse.builder()
                .page(page)          // 요청받은 page...
                .size(size)          // 요청받은 size...
                .total(3)            // 전체 건수 하드코딩(예: 3건)
                .items(items)
                .build();

        RsData<MyPaymentsResponse> body =
                new RsData<>("200", "내 결제 내역이 조회되었습니다.", data);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/payments/me/{paymentId}")
    @Operation(summary = "내 결제 상세 내역 조회")
    public ResponseEntity<RsData<MyPaymentResponse>> getMyPaymentDetail(
            @PathVariable Long paymentId
    ) {
        MyPaymentResponse data = MyPaymentResponse.builder()
                .paymentId(paymentId)
                .paymentMethodId(12L)
                .status("SUCCESS")
                .amount(50000L)
                .currency("KRW")
                .provider("toss")
                .methodType("CARD")
                .transactionId("pg_tx_abc123")
                .createdAt("2025-09-23T12:35:10Z")
                .modifyDate("2025-09-23T12:35:10Z")
                .idempotencyKey("topup-20250923-uid123-001")
                .cashTransactionId(98765L)
                .balanceAfter(155000L)
                .build();

        RsData<MyPaymentResponse> body =
                new RsData<>("200", "결제 상세 내역이 조회되었습니다.", data);

        return ResponseEntity.ok(body);
    }
}
