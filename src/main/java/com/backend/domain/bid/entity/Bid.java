package com.backend.domain.bid.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bids")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bid extends BaseEntity {

    @Column(name = "bid_price", nullable = false)
    private Long bidPrice;

    @Column(nullable = false, length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", insertable = false, updatable = false)
    private Member member;
}