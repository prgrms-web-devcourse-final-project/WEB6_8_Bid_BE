package com.backend.domain.product.entity;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductModifyRequest;
import com.backend.domain.product.enums.AuctionDuration;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.review.entity.Review;
import com.backend.global.exception.ServiceException;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> productImages = new ArrayList<>();

    @OneToOne(mappedBy = "product")
    private Review review;


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


//    public Long getBiddersCount() {
//        if (bids == null) return 0L;
//
//        return bids.stream()
//                .map(bid -> bid.getMember().getId())
//                .distinct()
//                .count();
//    }

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

    public String getStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            status = AuctionStatus.BEFORE_START.getDisplayName();
        } else if (now.isBefore(endTime)) {
            status = AuctionStatus.BIDDING.getDisplayName();
        } else {
            status = hasWinner() ? AuctionStatus.SUCCESSFUL.getDisplayName() : AuctionStatus.FAILED.getDisplayName();
        }

        return status;
    }

    public boolean hasWinner() {
        return endTime.isBefore(LocalDateTime.now()) && bids != null && !bids.isEmpty();
    }

    public void checkActorCanModify(Member actor) {
        if (!actor.equals(seller)) {
            throw new ServiceException("403", "상품 수정 권한이 없습니다.");
        }
    }

    public void checkActorCanDelete(Member actor) {
        if (!actor.equals(seller)) {
            throw new ServiceException("403", "상품 삭제 권한이 없습니다.");
        }
    }
}