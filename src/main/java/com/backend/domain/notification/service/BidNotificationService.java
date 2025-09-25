package com.backend.domain.notification.service;

import com.backend.domain.product.entity.Product;
import com.backend.global.webSocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidNotificationService {

    private final WebSocketService webSocketService;

    //  입찰 성공 알림
    public void notifyBidSuccess(Long userId, Product product, Long bidAmount) {
        String message = String.format("'%s' 상품에 %,d원으로 입찰했습니다.", 
            product.getProductName(), bidAmount);
            
        Map<String, Object> data = Map.of(
            "type", "BID_SUCCESS",
            "productId", product.getId(),
            "productName", product.getProductName(),
            "bidAmount", bidAmount
        );

        webSocketService.sendNotificationToUser(userId.toString(), message, data);
        log.info("입찰 성공 알림 전송 - 사용자: {}, 상품: {}, 금액: {}", userId, product.getId(), bidAmount);
    }

    //  입찰 밀림 알림 (더 높은 입찰이 들어왔을 때)
    public void notifyBidOutbid(Long userId, Product product, Long myBidAmount, Long newHighestBid) {
        String message = String.format("'%s' 상품에서 새로운 입찰(%,d원)이 들어와 밀렸습니다.", 
            product.getProductName(), newHighestBid);
            
        Map<String, Object> data = Map.of(
            "type", "BID_OUTBID",
            "productId", product.getId(),
            "productName", product.getProductName(),
            "myBidAmount", myBidAmount,
            "newHighestBid", newHighestBid
        );

        webSocketService.sendNotificationToUser(userId.toString(), message, data);
        log.info("입찰 밀림 알림 전송 - 사용자: {}, 상품: {}", userId, product.getId());
    }

    // 경매 종료 - 낙찰 알림
    public void notifyAuctionWon(Long winnerId, Product product, Long finalPrice) {
        String message = String.format("축하합니다! '%s' 상품을 %,d원에 낙찰받았습니다!", 
            product.getProductName(), finalPrice);
            
        Map<String, Object> data = Map.of(
            "type", "AUCTION_WON",
            "productId", product.getId(),
            "productName", product.getProductName(),
            "finalPrice", finalPrice
        );

        webSocketService.sendNotificationToUser(winnerId.toString(), message, data);
        log.info("낙찰 알림 전송 - 사용자: {}, 상품: {}, 낙찰가: {}", winnerId, product.getId(), finalPrice);
    }

    // 경매 종료 - 유찰 알림
    public void notifyAuctionLost(Long userId, Product product, Long finalPrice, Long myBidAmount) {
        String message = String.format("'%s' 상품 경매가 종료되었습니다. 최종 낙찰가는 %,d원입니다.", 
            product.getProductName(), finalPrice);
            
        Map<String, Object> data = Map.of(
            "type", "AUCTION_LOST",
            "productId", product.getId(),
            "productName", product.getProductName(),
            "finalPrice", finalPrice,
            "myBidAmount", myBidAmount
        );

        webSocketService.sendNotificationToUser(userId.toString(), message, data);
        log.info("낙찰 실패 알림 전송 - 사용자: {}, 상품: {}", userId, product.getId());
    }
}
