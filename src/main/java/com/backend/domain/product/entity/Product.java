package com.backend.domain.product.entity;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.entity.Payment;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.review.entity.Review;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Column(name = "product_name", nullable = false, length = 50)
    private String productName;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private ProductCategory category;

    @Column(name = "initial_price", nullable = false)
    private Long initialPrice;

    @Column(name = "current_price")
    private Long currentPrice;

    @Column(name = "auction_start_time", nullable = false)
    private LocalDateTime auctionStartTime;

    @Column(name = "auction_end_time", nullable = false)
    private LocalDateTime auctionEndTime;

    @Column(name = "auction_duration", nullable = false)
    private Integer auctionDuration;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private AuctionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Payment> payments;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Bid> bids;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> productImages;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Review> reviews;
}