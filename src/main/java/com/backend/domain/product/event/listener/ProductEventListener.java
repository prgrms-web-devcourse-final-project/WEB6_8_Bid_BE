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

/**
 * 상품 이벤트 리스너
 * - 상품의 특정 필드 변경 시 발생하는 이벤트 처리
 * - Elasticsearch 부분 업데이트를 통한 효율적인 동기화
 * - 비동기 처리로 메인 트랜잭션 성능 영향 최소화
 *
 * 처리 방식:
 * - @TransactionalEventListener: 트랜잭션 커밋 후 이벤트 처리
 * - @Async: 비동기로 처리하여 메인 로직에 영향 없음
 * - 실패 시에도 메인 트랜잭션은 정상 완료
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {
    
    private final ProductSyncService productSyncService;

    /**
     * 가격 변경 이벤트 처리
     * - 입찰로 인한 currentPrice 변경 시 발생
     * - Elasticsearch의 currentPrice 필드만 부분 업데이트
     *
     * 발생 시점:
     * - 새로운 입찰이 들어왔을 때
     * - 현재가보다 높은 금액으로 입찰 성공 시
     *
     * @param event 가격 변경 이벤트 (productId, oldPrice, newPrice)
     */
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

    /**
     * 상태 변경 이벤트 처리
     * - 경매 상태(status) 변경 시 발생
     * - Elasticsearch의 status 필드만 부분 업데이트
     *
     * 발생 시점:
     * - 경매 시작 전 -> 경매 중 (스케줄러)
     * - 경매 중 -> 낙찰/유찰 (스케줄러)
     *
     * @param event 상태 변경 이벤트 (productId, oldStatus, newStatus)
     */
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

    /**
     * 입찰자 수 변경 이벤트 처리
     * - 고유 입찰자 수(bidderCount) 변경 시 발생
     * - Elasticsearch의 bidderCount 필드만 부분 업데이트
     *
     * 발생 시점:
     * - 처음 입찰한 회원의 입찰 (bidderCount 증가)
     * - 동일 회원이 여러 번 입찰하는 경우는 증가하지 않음
     *
     * 활용:
     * - 인기순 정렬에 사용 (bidderCount 기준)
     * - 상품 목록/상세 조회에서 입찰자 수 표시
     *
     * @param event 입찰자 수 변경 이벤트 (productId, oldBidderCount, newBidderCount)
     */
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