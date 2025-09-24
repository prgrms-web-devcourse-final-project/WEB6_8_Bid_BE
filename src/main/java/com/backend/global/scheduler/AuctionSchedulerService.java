package com.backend.global.scheduler;

import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSchedulerService {

    private final EntityManager entityManager;
    private final BidRepository bidRepository;

    //  매분마다 실행되어 종료된 경매들을 확인하고 낙찰 처리
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void processEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        
        // 종료 시간이 지났지만 아직 BIDDING 상태인 경매들 조회
        List<Product> endedAuctions = entityManager.createQuery(
                "SELECT p FROM Product p WHERE p.endTime <= :now AND p.status = :status", 
                Product.class)
                .setParameter("now", now)
                .setParameter("status", AuctionStatus.BIDDING.getDisplayName())
                .getResultList();

        log.info("종료된 경매 {}건을 처리합니다.", endedAuctions.size());

        for (Product product : endedAuctions) {
            processAuctionEnd(product);
        }
    }

    // 개별 경매 종료 처리
    private void processAuctionEnd(Product product) {
        try {
            // 최고 입찰가 조회
            Long highestBidPrice = bidRepository.findHighestBidPrice(product.getId()).orElse(0L);
            
            if (highestBidPrice > 0) {
                // 입찰이 있었던 경우 - 낙찰 처리
                product.setStatus(AuctionStatus.SUCCESSFUL.getDisplayName());
                product.setCurrentPrice(highestBidPrice);
                log.info("상품 ID: {}, 낙찰가: {}원으로 낙찰 처리되었습니다.", 
                    product.getId(), highestBidPrice);
            } else {
                // 입찰이 없었던 경우 - 유찰 처리
                product.setStatus(AuctionStatus.FAILED.getDisplayName());
                log.info("상품 ID: {}, 입찰이 없어 유찰 처리되었습니다.", product.getId());
            }
            
            entityManager.merge(product);
            
        } catch (Exception e) {
            log.error("경매 종료 처리 중 오류 발생. 상품 ID: {}, 오류: {}", 
                product.getId(), e.getMessage(), e);
        }
    }
}
