package com.backend.domain.payment.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.constant.PaymentStatus;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    // 누가 어떤 수단으로 결제했는지..
    @ManyToOne(fetch = FetchType.LAZY)               // 여러 결제가 하나의 수단을 참조..
    @JoinColumn(name = "payment_method_id", nullable = false) // FK 컬럼..
    private PaymentMethod paymentMethod;             // 결제수단(카드/계좌)..

    @ManyToOne(fetch = FetchType.LAZY)               // 여러 결제가 한 회원과 연결
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                           // 결제한 사람..

    // 돈 정보..
    @Column(nullable = false)
    private Long amount;                             // 결제 금액..

    @Column(length = 5, nullable = false)
    private String currency;                         // 통화(예: "KRW")..

    // 상태..
    @Enumerated(EnumType.STRING)                     // 글자로 저장(PENDING 등)..
    @Column(length = 16, nullable = false)
    private PaymentStatus status;                    // 결제 상태..

    // PG(결제회사)에서 온 정보 스냅샷..
    @Column(length = 100)
    private String transactionId;                    // PG 트랜잭션 아이디..

    @Column(length = 50)
    private String provider;                         // PG 이름(예: toss)..

    @Column(length = 16)
    private String methodType;                       // 수단 종류 (CARD/BANK)...

    @Column(length = 64, unique = true)
    private String idempotencyKey;                   // 중복 결제 막는 키(멱등성)..

}
