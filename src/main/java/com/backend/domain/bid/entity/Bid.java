package com.backend.domain.bid.entity;

import com.backend.domain.bid.enums.BidStatus;
import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid extends BaseEntity {

    @Column(name = "bid_price", nullable = false)
    private Long bidPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BidStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id")
    private Member member;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_amount")
    private Long paidAmount;
}
