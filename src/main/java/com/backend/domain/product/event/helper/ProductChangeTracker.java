// domain/product/helper/ProductChangeTracker.java
package com.backend.domain.product.event.helper;

import com.backend.domain.product.entity.Product;
import com.backend.domain.product.event.ProductBidderCountChangedEvent;
import com.backend.domain.product.event.ProductPriceChangedEvent;
import com.backend.domain.product.event.ProductStatusChangedEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 상품 변경 추적 헬퍼 클래스
 * - 상품의 특정 필드 변경 여부를 추적하고 이벤트 발행
 * - 변경된 필드만 선택적으로 이벤트 발행 (효율성)
 * - 입찰 처리나 스케줄러에서 사용
 *
 * 사용 패턴:
 * 1. 변경 전 상태 스냅샷 생성: ProductChangeTracker.of(product)
 * 2. 비즈니스 로직 수행 (가격 변경, 상태 변경 등)
 * 3. 변경 후 이벤트 발행: tracker.publishChanges(publisher, product)
 *
 * 추적 필드:
 * - currentPrice: 입찰로 인한 가격 변경
 * - status: 경매 상태 변경
 * - bidderCount: 입찰자 수 변경
 */
@Getter
public class ProductChangeTracker {
    private final Long productId;
    private final Long oldPrice;
    private final String oldStatus;
    private final Integer oldBidderCount;

    /**
     * private 생성자
     * - 팩토리 메서드(of)를 통해서만 생성 가능
     * - 생성 시점의 상품 상태를 스냅샷으로 저장
     *
     * @param product 추적할 상품
     */
    private ProductChangeTracker(Product product) {
        this.productId = product.getId();
        this.oldPrice = product.getCurrentPrice();
        this.oldStatus = product.getStatus();
        this.oldBidderCount = product.getBidderCount();
    }

    /**
     * ProductChangeTracker 생성 팩토리 메서드
     * - 현재 상품 상태를 스냅샷으로 저장
     * - 이후 변경 사항을 감지하기 위한 기준점 설정
     *
     * @param product 추적할 상품
     * @return 생성된 ProductChangeTracker
     */
    public static ProductChangeTracker of(Product product) {
        return new ProductChangeTracker(product);
    }

    /**
     * 변경된 필드만 선택적으로 이벤트 발행
     * - 각 필드를 변경 전 스냅샷과 비교
     * - 변경된 필드가 있는 경우에만 해당 이벤트 발행
     * - 불필요한 이벤트 발행 및 Elasticsearch 업데이트 방지
     *
     * @param publisher Spring의 ApplicationEventPublisher
     * @param product 변경 후 상품 상태
     */
    public void publishChanges(ApplicationEventPublisher publisher, Product product) {
        // 가격 변경
        if (!oldPrice.equals(product.getCurrentPrice())) {
            publisher.publishEvent(new ProductPriceChangedEvent(
                productId, 
                oldPrice, 
                product.getCurrentPrice()
            ));
        }

        // 상태 변경
        if (!oldStatus.equals(product.getStatus())) {
            publisher.publishEvent(new ProductStatusChangedEvent(
                productId, 
                oldStatus, 
                product.getStatus()
            ));
        }

        // 입찰자 수 변경
        if (!oldBidderCount.equals(product.getBidderCount())) {
            publisher.publishEvent(new ProductBidderCountChangedEvent(
                productId,
                oldBidderCount,
                product.getBidderCount()
            ));
        }
    }
}