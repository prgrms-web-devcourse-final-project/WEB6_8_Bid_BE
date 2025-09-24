package com.backend.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

// 결제 수단 다건/단건 조회..
public class PaymentMethodResponse {

    // 공통..
    private final Long id;
    private final String type;       // "CARD" / "BANK"..
    private final String alias;      // 별명..
    private final Boolean isDefault; // 자주 사용하는 거 등록..

    // 카드 전용..
    private final String brand;      // 카드 브랜드(예: SHINHAN)..
    private final String last4;      // 카드 끝 4자리..
    private final Integer expMonth;  // 유효월..
    private final Integer expYear;   // 유효년..

    // 계좌 전용..
    private final String bankCode;   // 은행 코드(예: 004)..
    private final String bankName;   // 은행 이름(예: KB국민은행)..
    private final String acctLast4;  // 계좌 끝 4자리..

    // 시간..
    private final String createDate; // 생성..
    private final String modifyDate; // 변경..

    private final String expireDate; // "YYYY-MM"
}