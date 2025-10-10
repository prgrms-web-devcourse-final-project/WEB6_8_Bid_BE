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

    // 상품 정보 수정
    public void modify(ProductModifyRequest request) {
        this.productName = request.name();
        this.description = request.description();
        this.category = ProductCategory.fromId(request.categoryId());
        this.initialPrice = request.initialPrice();
        this.startTime = request.auctionStartTime();
        this.duration = AuctionDuration.fromValue(request.auctionDuration());
        this.endTime = this.startTime.plusHours(this.duration);
        this.deliveryMethod = request.deliveryMethod();
        this.location = request.location();
    }

    // ======================================= image methods ======================================= //
    public void addProductImage(ProductImage productImage) {
        productImages.add(productImage);

        // 첫 번째 이미지는 썸네일로 자동 설정
        if (thumbnailUrl == null) {
            this.thumbnailUrl = productImage.getImageUrl();
        }
    }

    public void deleteProductImage(ProductImage productImage) {
        productImages.remove(productImage);

        // 삭제된 이미지가 썸네일이면 null로 설정
        if (thumbnailUrl.equals(productImage.getImageUrl())) {
            thumbnailUrl = null;
        }
    }

    public String getThumbnail() {
        if (thumbnailUrl != null) {
            return thumbnailUrl;
        }

        // 썸네일이 없으면 첫 번째 이미지 찾아서 설정
        thumbnailUrl = productImages.stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return thumbnailUrl;
    }

    // ======================================= bid methods ======================================= //
    /**
     * 낙찰자 조회
     * - 경매가 종료되고 낙찰 상태일 때만 반환
     * - 현재 최고가와 동일한 입찰가를 가진 입찰자
     */
    public Member getBidder() {
        // 낙찰 상태가 아니거나 아직 종료되지 않았으면 null 반환
        if (!status.equals(AuctionStatus.SUCCESSFUL.getDisplayName()) || endTime.isAfter(LocalDateTime.now())) {
            return null;
        }

        // 현재 최고가와 동일한 입찰을 찾아서 입찰자 반환
        return bids.stream()
//                    .max(Comparator.comparing(Bid::getBidPrice))
                .filter(bid -> bid.getBidPrice().equals(currentPrice))
                .map(Bid::getMember)
                .findFirst()
                .orElse(null);
    }

    /**
     * 입찰 추가 및 입찰자 수 업데이트
     * - 양방향 관계 설정
     * - 고유 입찰자 수 자동 계산 (동일 회원의 중복 입찰 제거)
     */
    public void addBid(Bid bid) {
        bids.add(bid);

        // 중복 제거한 입찰자 수 계산
        int _bidderCount = (int) bids.stream()
                .map(b -> b.getMember().getId())
                .distinct()
                .count();

        // 입찰자 수가 변경된 경우에만 업데이트
        if (_bidderCount != bidderCount) {
            bidderCount = _bidderCount;
        }
    }

    // ======================================= auth methods ======================================= //
    // 상품 수정 권한 검증 (판매자 본인만 수정 가능)
    public void checkActorCanModify(Member actor) {
        if (!actor.equals(seller)) {
            throw ProductException.accessModifyForbidden();
        }
    }

    // 상품 삭제 권한 검증 (판매자 본인만 삭제 가능)
    public void checkActorCanDelete(Member actor) {
        if (!actor.equals(seller)) {
            throw ProductException.accessDeleteForbidden();
        }
    }

    // ======================================= other methods ======================================= //
    /**
     * 테스트 전용 빌더
     * - 프로덕션 코드에서는 사용 금지
     * - ID를 포함한 모든 필드를 직접 설정 가능
     * - 단위 테스트에서 목 데이터 생성용
     */
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