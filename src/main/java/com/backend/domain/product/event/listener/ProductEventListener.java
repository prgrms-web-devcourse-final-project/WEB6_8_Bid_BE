package com.backend.domain.product.event.listener;

import com.backend.domain.product.event.ProductBidderCountChangedEvent;
import com.backend.domain.product.event.ProductPriceChangedEvent;
import com.backend.domain.product.event.ProductStatusChangedEvent;
import com.backend.domain.product.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {
    
    private final ProductSyncService productSyncService;

    // 가격 변경 이벤트 처리
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePriceChanged(ProductPriceChangedEvent event) {
        log.debug("가격 변경 이벤트: productId={}, {}원 -> {}원", 
            event.productId(), event.oldPrice(), event.newPrice());
        
        productSyncService.syncProductPriceUpdate(
            event.productId(),
            event.newPrice()
        );
    }

    // 상태 변경 이벤트 처리
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStatusChanged(ProductStatusChangedEvent event) {
        log.debug("상태 변경 이벤트: productId={}, {} -> {}", 
            event.productId(), event.oldStatus(), event.newStatus());
        
        productSyncService.syncProductStatusUpdate(
            event.productId(),
            event.newStatus()
        );
    }

    // 입찰자 수 변경 이벤트 처리
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBidderCountChanged(ProductBidderCountChangedEvent event) {
        log.debug("입찰자 수 변경 이벤트: productId={}, bidderCount={}", 
            event.productId(), event.newBidderCount());
        
        productSyncService.syncProductBidderCountUpdate(
            event.productId(),
            event.newBidderCount()
        );
    }
}