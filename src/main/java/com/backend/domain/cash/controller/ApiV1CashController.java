package com.backend.domain.cash.controller;

import com.backend.domain.cash.dto.CashResponse;
import com.backend.domain.cash.dto.CashTransactionItemResponse;
import com.backend.domain.cash.dto.CashTransactionResponse;
import com.backend.domain.cash.dto.CashTransactionsResponse;
import com.backend.global.rsData.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/cashs")
public class ApiV1CashController {

    // 내 지갑 잔액 조회..
    @GetMapping("/cash")
    public ResponseEntity<RsData<CashResponse>> getMyCash() {

        CashResponse data = CashResponse.builder()
                .cashId(77L)                          // Cash.id..
                .memberId(123L)
                .balance(155000L)                    // 현재 잔액(원)..
                .createDate("2025-08-01T10:00:00Z")   // 생성 시각..
                .modifyDate("2025-09-23T12:35:10Z")   // 최근 갱신 시각..
                .build();

        RsData<CashResponse> body =
                new RsData<>("200", "지갑 잔액이 조회되었습니다.", data);

        return ResponseEntity.ok(body);
    }

    // 내 원장 목록(입금/출금 내역)..
    @GetMapping("/cash/transactions")
    public ResponseEntity<RsData<CashTransactionsResponse>> getCashTransactions(
            @RequestParam(defaultValue = "1") int page,   // 몇 페이지..
            @RequestParam(defaultValue = "20") int size   // 몇 개씩..
    ) {
        // 충전(입금)..
        CashTransactionItemResponse t1 = CashTransactionItemResponse.builder()
                .transactionId(98765L)
                .cashId(77L)
                .type("DEPOSIT")
                .amount(50000L)                          // 입금 = 양수..
                .balanceAfter(155000L)
                .createdAt("2025-09-23T12:35:10Z")
                .related(CashTransactionItemResponse.Related.builder()
                        .type("PAYMENT")
                        .id(101L)
                        .summary("지갑 충전(toss, CARD)")
                        .build())
                .build();

        // 낙찰 결제(출금)..
        CashTransactionItemResponse t2 = CashTransactionItemResponse.builder()
                .transactionId(99001L)
                .cashId(77L)
                .type("WITHDRAW")
                .amount(-32000L)                         // 출금 = 음수..
                .balanceAfter(123000L)
                .createdAt("2025-09-30T21:03:00Z")
                .related(CashTransactionItemResponse.Related.builder()
                        .type("BID")
                        .id(3456L)
                        .product(CashTransactionItemResponse.Product.builder()
                                .productId(3L)
                                .productName("에어팟 프로 2세대")
                                .thumbnailUrl("https://.../p3.jpg")
                                .build())
                        .summary("낙찰 결제 - 에어팟 프로 2세대")
                        .build())
                .build();

        // 목록 + 페이징..
        List<CashTransactionItemResponse> items = List.of(t1, t2);
        CashTransactionsResponse data = CashTransactionsResponse.builder()
                .page(page)
                .size(size)
                .total(2)
                .items(items)
                .build();

        RsData<CashTransactionsResponse> body =
                new RsData<>("200", "지갑 원장 목록이 조회되었습니다.", data);

        return ResponseEntity.ok(body);
    }

    // 내 원장 단건 상세..
    @GetMapping("/cash/transactions/{transactionId}")
    public ResponseEntity<RsData<CashTransactionResponse>> getCashTransactionDetail(
            @PathVariable Long transactionId
    ) {
        CashTransactionResponse data = CashTransactionResponse.builder()
                .transactionId(transactionId)
                .cashId(77L)
                .type("DEPOSIT")
                .amount(50000L)
                .balanceAfter(155000L)
                .createdAt("2025-09-23T12:35:10Z")
                .related(CashTransactionResponse.Related.builder()
                        .type("PAYMENT")
                        .id(101L)
                        .links(CashTransactionResponse.Links.builder()
                                .paymentDetail("/payments/me/101") // 바로가기..
                                .build())
                        .build())
                .build();

        RsData<CashTransactionResponse> body =
                new RsData<>("200", "지갑 원장 상세가 조회되었습니다.", data);

        return ResponseEntity.ok(body);
    }
}
