package com.backend.domain.product.entity;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.entity.Payment;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.review.entity.Review;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.LocalDateTime.now;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
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

    @Setter
    @Column(name = "current_price")
    private Long currentPrice;

    @Column(name = "auction_start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "auction_end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "auction_duration", nullable = false)
    private Integer duration;

    @Column(length = 50, nullable = false)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false)
    private DeliveryMethod deliveryMethod;

    @Column(length = 50, nullable = false)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Payment> payments;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Bid> bids;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> productImages = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();


    public Product(String productName, String description, ProductCategory category, Long initialPrice, LocalDateTime startTime, Integer duration, DeliveryMethod deliveryMethod, String location, Member seller) {
        this.productName = productName;
        this.description = description;
        this.category = category;
        this.initialPrice = initialPrice;
        this.currentPrice = initialPrice;
        this.startTime = startTime;
        this.endTime = startTime.plusHours(duration);
        this.duration = duration;
        this.deliveryMethod = deliveryMethod;
        this.location = location;
        this.seller = seller;

        if (startTime.isBefore(now()) || startTime.isEqual(now())) {
            this.status = AuctionStatus.BIDDING.getDisplayName();
        } else {
            this.status = AuctionStatus.BEFORE_START.getDisplayName();
        }
    }


    public Long getBiddersCount() {
        if (bids == null) return 0L;

        return bids.stream()
                .map(bid -> bid.getMember().getId())
                .distinct()
                .count();
    }

    public void addProductImage(ProductImage productImage) {
        this.productImages.add(productImage);
        productImage.setProduct(this);
    }
}