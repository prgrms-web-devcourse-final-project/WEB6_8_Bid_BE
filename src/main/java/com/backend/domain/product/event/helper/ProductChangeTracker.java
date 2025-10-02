// domain/product/helper/ProductChangeTracker.java
package com.backend.domain.product.event.helper;

import com.backend.domain.product.entity.Product;
import com.backend.domain.product.event.ProductBidderCountChangedEvent;
import com.backend.domain.product.event.ProductPriceChangedEvent;
import com.backend.domain.product.event.ProductStatusChangedEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;

@Getter
public class ProductChangeTracker {
    private final Long productId;
    private final Long oldPrice;
    private final String oldStatus;
    private final Integer oldBidderCount;

    private ProductChangeTracker(Product product) {
        this.productId = product.getId();
        this.oldPrice = product.getCurrentPrice();
        this.oldStatus = product.getStatus();
        this.oldBidderCount = product.getBidderCount();
    }

    public static ProductChangeTracker of(Product product) {
        return new ProductChangeTracker(product);
    }

    // 변경된 필드만 이벤트 발행
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