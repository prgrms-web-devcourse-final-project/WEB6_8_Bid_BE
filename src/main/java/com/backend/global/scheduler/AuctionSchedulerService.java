package com.backend.global.scheduler;

import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.notification.service.AuctionNotificationService;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.global.websocket.service.WebSocketService;
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
    private final WebSocketService webSocketService;
    private final AuctionNotificationService auctionNotificationService;

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

    // 매분마다 실행되어 10분 후 종료 예정인 경매들에 종료 임박 알림 전송
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void processAuctionsEndingSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesLater = now.plusMinutes(10);
        
        // 10분 후 종료 예정인 BIDDING 상태 경매들 조회 (정확히 10분 전 알림을 위해 1분 범위로 조회)
        List<Product> endingSoonAuctions = entityManager.createQuery(
                "SELECT p FROM Product p WHERE p.endTime BETWEEN :tenMinutesLater AND :elevenMinutesLater AND p.status = :status", 
                Product.class)
                .setParameter("tenMinutesLater", tenMinutesLater)
                .setParameter("elevenMinutesLater", tenMinutesLater.plusMinutes(1))
                .setParameter("status", AuctionStatus.BIDDING.getDisplayName())
                .getResultList();

        log.info("종료 임박 경매 {}건을 처리합니다.", endingSoonAuctions.size());

        for (Product product : endingSoonAuctions) {
            processAuctionEndingSoon(product);
        }
    }

    // 매분마다 실행되어 시작된 경매들을 확인/처리
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void processStartingAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 시작 시간이 지났지만 아직 BEFORE_START 상태인 경매들 조회
        List<Product> startingAuctions = entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.startTime <= :now AND p.status = :status",
                        Product.class)
                .setParameter("now", now)
                .setParameter("status", AuctionStatus.BEFORE_START.getDisplayName())
                .getResultList();

        log.info("시작된 경매 {}건을 처리합니다.", startingAuctions.size());

        for (Product product : startingAuctions) {
            processAuctionStart(product);
        }
    }

    // 상품 구독하고 있는 개별 사용자들에게 브로드캐스트 알림 전송
    private void processAuctionEndingSoon(Product product) {
        try {
            webSocketService.broadcastAuctionEndingSoon(product.getId(), product.getProductName());
            log.info("상품 ID: {} ({})에 대해 종료 임박 브로드캐스트 알림을 전송했습니다.",
                    product.getId(), product.getProductName());

        } catch (Exception e) {
            log.error("경매 종료 임박 처리 중 오류 발생. 상품 ID: {}, 오류: {}",
                    product.getId(), e.getMessage(), e);
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
                
                // 구독자들에게 낙찰 알림 전송
                webSocketService.broadcastAuctionEnd(product.getId(), true, highestBidPrice);
                
            } else {
                // 입찰이 없었던 경우 - 유찰 처리
                product.setStatus(AuctionStatus.FAILED.getDisplayName());
                log.info("상품 ID: {}, 입찰이 없어 유찰 처리되었습니다.", product.getId());
                
                // 구독자들에게 유찰 알림 전송
                webSocketService.broadcastAuctionEnd(product.getId(), false, 0L);
            }
            
            entityManager.merge(product);
            
        } catch (Exception e) {
            log.error("경매 종료 처리 중 오류 발생. 상품 ID: {}, 오류: {}", 
                product.getId(), e.getMessage(), e);
        }
    }

    // 개별 경매 시작 처리
    private void processAuctionStart(Product product) {
        try {
            // 상태 업데이트
            product.setStatus(AuctionStatus.BIDDING.getDisplayName());
            log.info("상품 ID: {} ({}) 경매가 시작되었습니다.", product.getId(), product.getProductName());

            // 판매자에게 경매 시작 알림 전송
            webSocketService.broadcastAuctionStart(product.getId(), product.getProductName());

            entityManager.merge(product);

        } catch (Exception e) {
            log.error("경매 시작 처리 중 오류 발생. 상품 ID: {}, 오류: {}",
                    product.getId(), e.getMessage(), e);
        }
    }
}
