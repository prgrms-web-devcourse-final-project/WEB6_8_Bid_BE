package com.backend.domain.payment.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.enums.PaymentMethodType;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

// 지우기(delete) 버튼”을 눌러도, 진짜로 종이를 버리는 게 아니라 삭제됨 스티커를 붙여서 서랍에 그대로 보관해 둠..
@SQLDelete(sql = "UPDATE payment_method SET deleted = true, active = false WHERE id = ?")
public class PaymentMethod extends BaseEntity {

    // 이 결제수단의 주인(누구꺼인지)..
    @ManyToOne(fetch = FetchType.LAZY)                                 // 여러 결제수단이 한 회원에 속할 수 있음..
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 결제수단의 종류(CARD/BANK)...
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PaymentMethodType methodType;                                    // 카드인지, 계좌인지..

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

    @Column(length = 32, nullable = false)
    private String provider;                                          // 어떤 PG를 통해 결제하는지(예: "toss", "iamport")..

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;                                    // 사용 가능 여부(기본은 true)..

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;                                  // 지워진 것처럼 보이게만 하고, 원장/영수증 보존, 추후 정산 이슈 방지를 위해..
}
