package com.backend.domain.payment.controller;

import com.backend.domain.payment.dto.*;
import com.backend.global.rsData.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class MockPaymentController {


    // 결제 수단 다건(목록) 조회..
    @GetMapping("/paymentMethods")
    public ResponseEntity<RsData<List<PaymentMethodResponse>>> getPaymentMethods() {

        PaymentMethodResponse card = PaymentMethodResponse.builder()
                .id(1L)
                .type("신용카드")
                .aliasName("주거래 카드")
                .isDefault(true)
                .createDate(LocalDateTime.parse("2024-12-01T10:00:00"))
                .cardCompany("신한카드")
                .cardNumber("**** **** **** 1234")
                .expireDate("2027-12")
                .build();

        PaymentMethodResponse account = PaymentMethodResponse.builder()
                .id(2L)
                .type("계좌이체")
                .aliasName("급여통장")
                .isDefault(false)
                .createDate(LocalDateTime.parse("2024-11-25T15:30:00"))
                .bankName("KB국민은행")
                .accountNumber("******1234")
                .build();

        List<PaymentMethodResponse> data = List.of(card, account);
        RsData<List<PaymentMethodResponse>> response = new RsData<>("200", "결제수단 목록이 조회되었습니다.", data);

        return ResponseEntity.ok(response);
    }


    // 결제 수단 단건(상세) 조회..
    @GetMapping("/paymentMethods/{id}")
    public ResponseEntity<RsData<PaymentMethodResponse>> getPaymentMethod(@PathVariable Long id) {

        PaymentMethodResponse data = PaymentMethodResponse.builder()
                .id(id)
                .type("신용카드")
                .aliasName("주거래 카드")
                .isDefault(true)
                .createDate(LocalDateTime.parse("2024-12-01T10:00:00"))
                .cardCompany("신한카드")
                .cardNumber("**** **** **** 1234")
                .expireDate("2027-12")
                .build();

        RsData<PaymentMethodResponse> response = new RsData<>("200", "결제수단이 조회되었습니다.", data);

        return ResponseEntity.ok(response);
    }

    // 결제 수단 등록..
    @PostMapping("/paymentMethods")
    public ResponseEntity<RsData<PaymentMethodResponse>> createPaymentMethod(
            @RequestBody PaymentMethodCreateRequest request) {

        PaymentMethodResponse data = PaymentMethodResponse.builder()
                .id(3L)
                .type(request.getType())
                .aliasName(request.getAliasName())
                .cardCompany(request.getCardCompany())
                .cardNumber("**** **** **** 5678") // 카드번호는 마스킹..
                .expireDate(request.getExpiryYear() + "-" + request.getExpiryMonth())
                .isDefault(request.getIsDefault())
                .createDate(LocalDateTime.now())
                .build();

        RsData<PaymentMethodResponse> response =
                new RsData<>("200", "결제수단이 등록되었습니다.", data);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 결제 수단 수정..
    @PutMapping("/paymentMethods/{id}")
    public ResponseEntity<RsData<PaymentMethodEditResponse>> editPaymentMethod(
            @PathVariable Long id,
            @RequestBody PaymentMethodEditRequest request) {

        PaymentMethodEditResponse data = PaymentMethodEditResponse.builder()
                .id(id)
                .type("신용카드")
                .aliasName(request.getAliasName())
                .cardCompany("신한카드")
                .cardNumber("**** **** **** 1234")
                .expireDate("2027-12")
                .isDefault(request.getIsDefault())
                .createDate(LocalDateTime.parse("2024-12-17T15:30:00"))
                .modifyDate(LocalDateTime.now())
                .build();

        RsData<PaymentMethodEditResponse> response =
                new RsData<>("200", "결제수단이 수정되었습니다.", data);

        return ResponseEntity.ok(response);
    }

    // 내 결제 상세 내역..
    @GetMapping("/payments/me/{paymentId}")
    public ResponseEntity<RsData<MyPaymentResponse>> getMyPaymentDetail(
            @PathVariable Long paymentId) {

        PaymentMethodInPaymentResponse paymentMethodInfo = PaymentMethodInPaymentResponse.builder()
                .type("신용카드")
                .aliasName("주거래 카드")
                .cardCompany("신한카드")
                .cardNumber("**** **** **** 1234")
                .build();

        MyPaymentResponse data = MyPaymentResponse.builder()
                .id(paymentId)
                .bidId(4L)
                .productName("아이폰 15 Pro 256GB")
                .amount(1200000)
                .sellerNickname("판매자닉네임")
                .status("COMPLETED")
                .paymentMethod(paymentMethodInfo)
                .paidDate(LocalDateTime.parse("2024-12-17T18:00:00"))
                .thumbnailUrl("/images/product1_1.jpg")
                .build();

        RsData<MyPaymentResponse> response =
                new RsData<>("200", "결제 상세 내역이 조회되었습니다.", data);

        return ResponseEntity.ok(response);
    }

    // 내 결제 내역..
    @GetMapping("/payments/me")
    public ResponseEntity<RsData<MyPaymentsResponse>> getMyPayments() {

        // 결제 내역..
        MyPaymentItemResponse payment1 = MyPaymentItemResponse.builder()
                .id(1L)
                .bidId(4L)
                .productName("아이폰 15 Pro 256GB")
                .amount(1200000)
                .sellerNickname("판매자닉네임")
                .status("COMPLETED")
                .paidDate(LocalDateTime.parse("2024-12-17T18:00:00"))
                .build();

        List<MyPaymentItemResponse> paymentList = List.of(payment1);

        // 페이징 정보..
        PageableResponse pageable = PageableResponse.builder()
                .currentPage(1)
                .pageSize(20)
                .totalPages(10)
                .totalElements(95L)
                .hasNext(true)
                .hasPrevious(false)
                .build();

        MyPaymentsResponse data = MyPaymentsResponse.builder()
                .content(paymentList)
                .pageable(pageable)
                .build();

        RsData<MyPaymentsResponse> response = new RsData<>("200", "결제 내역이 조회되었습니다.", data);

        return ResponseEntity.ok(response);
    }

    // 결제 요청..
    @PostMapping("/payments")
    public ResponseEntity<RsData<PaymentResponse>> requestPayment(
            @RequestBody PaymentRequest request) {

        PaymentMethodInPaymentResponse paymentMethodDetail = PaymentMethodInPaymentResponse.builder()
                .type("신용카드")
                .cardCompany("신한카드")
                .cardNumber("**** **** **** 1234")
                .build();

        PaymentResponse data = PaymentResponse.builder()
                .productId(3L)
                .amount(request.getAmount())
                .paymentMethod(paymentMethodDetail)
                .status("COMPLETED")
                .paidDate(LocalDateTime.parse("2024-12-17T16:30:00"))
                .build();

        RsData<PaymentResponse> response =
                new RsData<>("200", "결제 요청이 완료되었습니다.", data);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 결제 수단 삭제..
    @DeleteMapping("/paymentMethods/{id}")
    public ResponseEntity<RsData<Void>> deletePaymentMethod(
            @PathVariable Long id) {

        RsData<Void> response =
                new RsData<>("200", "결제수단이 삭제되었습니다.");

        return ResponseEntity.ok(response);
    }
}