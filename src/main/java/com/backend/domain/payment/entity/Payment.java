package com.backend.domain.payment.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.enums.PaymentStatus;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_payment_member_id_idempotency",
                        columnNames = {"member_id", "idempotency_key"} // 멱등키는 회원 단위로만 유니크(전역 유니크 X)..
                )
        }
)

public class Payment extends BaseEntity {

    // 누가 어떤 수단으로 결제했는지..
    @ManyToOne(fetch = FetchType.LAZY)               // 여러 결제가 하나의 수단을 참조..
    @JoinColumn(name = "payment_method_id", nullable = false) // FK 컬럼..
    private PaymentMethod paymentMethod;             // 결제수단(카드/계좌)..

    @ManyToOne(fetch = FetchType.LAZY)               // 여러 결제가 한 회원과 연결..
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                           // 결제한 사람..

    // 돈 정보..
    @Column(nullable = false)
    private Long amount;                             // 결제 금액..

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "KRW";                 // 통화(예: "KRW")..

    // 상태..
    @Enumerated(EnumType.STRING)                     // 글자로 저장(PENDING 등)..
    @Column(length = 16, nullable = false)
    private PaymentStatus status;                    // 결제 상태..

    // PG(결제회사)에서 온 정보 스냅샷..
    @Column(length = 191)
    private String transactionId;                    // PG 트랜잭션 아이디..

    @Column(length = 50, nullable = false)
    private String provider;                         // PG 이름(예: toss)..

    @Column(length = 16, nullable = false)
    private String methodType;                       // 수단 종류 (CARD/BANK)...

    @Column(name = "idempotency_key", length = 64, nullable = false) // 재시도/중복 클릭 방지..
    private String idempotencyKey;                   // 중복 결제 막는 키(멱등성)..

    private LocalDateTime paidAt;                   // 지갑 충전(입금)이 PG/결제에서 최종 승인된 시각..

    // 이후 PaymentMethod가 수정/삭제되어도 영수증(결제내역) 내용이 그대로 유지..
    // 필요한 이유 : 결제 후 수단 별칭/브랜드가 바뀌어도, 그때의 영수증은 그대로 보여야 함..
    @Column(length=100)
    private String methodAlias; // 예: "급여통장"

    @Column(length=50)
    private String cardBrand;   // 예: "SHINHAN"

    @Column(length=4)
    private String cardLast4;   // 카드 끝 4자리

    @Column(length=50)
    private String bankName;    // 예: "KB국민은행"

    @Column(length=4)
    private String bankLast4;   // 계좌 끝 4자리
}
