package com.backend.domain.product.entity;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.enums.AuctionDuration;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.review.entity.Review;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.LocalDateTime.now;

@Entity
@Table(name = "products", indexes = {
        // 기본 목록 조회 (상태, 최신순)
        @Index(name = "idx_status_create", columnList = "status, create_date DESC"),
        // 판매자 목록 조회 (회원, 상태, 최신순)
        @Index(name = "idx_seller_status_create", columnList = "seller_id, status, create_date DESC"),
        // 카테고리 목록 조회 (카테고리, 상태, 최신순)
        @Index(name = "idx_category_status_create", columnList = "category, status, create_date DESC"),
        // 지역 목록 조회 (지역, 상태, 최신순)
        @Index(name = "idx_location_status_create", columnList = "location, status, create_date DESC")
})
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

    @Setter
    @Column(name = "auction_end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "auction_duration", nullable = false)
    private Integer duration;

    @Setter
    @Column(length = 50, nullable = false)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false)
    private DeliveryMethod deliveryMethod;

    @Column(length = 50)
    private String location;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "bidder_count", nullable = false)
    private Integer bidderCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> productImages = new ArrayList<>();

    @OneToOne(mappedBy = "product")
    private Review review = null;


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


    public void addProductImage(ProductImage productImage) {
        productImages.add(productImage);

        if (thumbnailUrl == null) {
            this.thumbnailUrl = productImage.getImageUrl();
        }
    }

    public String getThumbnail() {
        if (thumbnailUrl != null) {
            return thumbnailUrl;
        }

        thumbnailUrl = productImages.stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return thumbnailUrl;
    }

    public void modify(ProductModifyRequest validatedRequest) {
        if (validatedRequest.name() != null) this.productName = validatedRequest.name();
        if (validatedRequest.description() != null) this.description = validatedRequest.description();
        if (validatedRequest.categoryId() != null) this.category = ProductCategory.fromId(validatedRequest.categoryId());
        if (validatedRequest.initialPrice() != null) this.initialPrice = validatedRequest.initialPrice();
        if (validatedRequest.auctionStartTime() != null) this.startTime = validatedRequest.auctionStartTime();
        if (validatedRequest.auctionDuration() != null) this.duration = AuctionDuration.fromValue(validatedRequest.auctionDuration());
        if (validatedRequest.deliveryMethod() != null) this.deliveryMethod = validatedRequest.deliveryMethod();
        if (validatedRequest.location() != null) this.location = validatedRequest.location();
    }

    public void deleteProductImage(ProductImage productImage) {
        productImages.remove(productImage);

        if (thumbnailUrl.equals(productImage.getImageUrl())) {
            thumbnailUrl = null;
        }
    }

    public void checkActorCanModify(Member actor) {
        if (!actor.equals(seller)) {
            throw ProductException.accessModifyForbidden();
        }
    }

    public void checkActorCanDelete(Member actor) {
        if (!actor.equals(seller)) {
            throw ProductException.accessDeleteForbidden();
        }
    }

    public Member getBidder() {
        if (!status.equals(AuctionStatus.SUCCESSFUL.getDisplayName()) || endTime.isAfter(LocalDateTime.now())) {
            return null;
        }

        return bids.stream()
//                    .max(Comparator.comparing(Bid::getBidPrice))
                .filter(bid -> bid.getBidPrice().equals(currentPrice))
                .map(Bid::getMember)
                .findFirst()
                .orElse(null);
    }

    public void addBid(Bid bid) {
        bids.add(bid);

        bidderCount = (int) bids.stream()
                .map(b -> b.getMember().getId())
                .distinct()
                .count();
    }

    // 테스트 전용 (프로덕션에서는 사용 금지)
    @Builder(builderMethodName = "testBuilder", buildMethodName = "testBuild")
    private Product(
            Long id, String productName, String description, ProductCategory category,
            Long initialPrice, Long currentPrice, LocalDateTime startTime,
            LocalDateTime endTime, Integer duration, String status,
            DeliveryMethod deliveryMethod, String location, Member seller
    ) {
        setId(id);
        this.productName = productName;
        this.description = description;
        this.category = category;
        this.initialPrice = initialPrice;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.status = status;
        this.deliveryMethod = deliveryMethod;
        this.location = location;
        this.seller = seller;
    }
}