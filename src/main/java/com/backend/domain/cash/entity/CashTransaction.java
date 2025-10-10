package com.backend.domain.cash.entity;

import com.backend.domain.cash.enums.CashTxType;
import com.backend.domain.cash.enums.RelatedType;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashTransaction extends BaseEntity {

    // 어떤 지갑의 거래인가요?
    @ManyToOne(fetch = FetchType.LAZY)                    // 한 지갑에 거래가 여러개..
    @JoinColumn(name = "cash_id", nullable = false)
    private Cash cash;

    // 거래 종류(입금/출금)..
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CashTxType type;                              // 거래 타입..

    // 돈 정보..
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;                            // 이 거래가 끝난 직후 잔액..

    // 왜 돈이 들어오고/나갔는지? (추적용)..
    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", length = 32, nullable = false)
    private RelatedType relatedType;                      // 예: "PAYMENT", "BID"..

    @Column(nullable=false)
    private Long relatedId;                               // 예: 결제ID, 입찰ID 등..

}
