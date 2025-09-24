package com.backend.domain.payment.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.constant.PaymentMethodType;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod extends BaseEntity {

    // 이 결제수단의 주인(누구꺼인지)..
    @ManyToOne(fetch = FetchType.LAZY)                                 // 여러 결제수단이 한 회원에 속할 수 있음..
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 결제수단의 종류(CARD/BANK)...
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PaymentMethodType type;                                    // 카드인지, 계좌인지..

    // 공통 정보..
    @Column(length = 200)
    private String token;                                              // PG에서 받은 토큰(필수 아님)..

    @Column(length = 100)
    private String alias;                                              // 별명(예: "급여통장", "결혼식 카드")..

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;                                         // 내가 주로 쓰는 결제수단(true/false)..

    // 카드 전용 정보..
    @Column(length = 50)
    private String brand;                                              // 카드 브랜드/발급사(예: SHINHAN)..

    @Column(length = 4)
    private String last4;                                              // 카드번호 끝 4자리(예: 1234)..

    private Integer expMonth;                                          // 유효기간(월) 1~12..
    private Integer expYear;                                           // 유효기간(년) 예: 2027..

    // 계좌 전용 정보..
    @Column(length = 10)
    private String bankCode;                                           // 은행 코드(예: 004)..

    @Column(length = 50)
    private String bankName;                                           // 은행 이름(예: KB국민은행)..

    @Column(length = 4)
    private String acctLast4;                                          // 계좌번호 끝 4자리(예: 5678)..

}
