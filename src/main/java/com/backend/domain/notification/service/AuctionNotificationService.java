package com.backend.domain.notification.service;

import com.backend.domain.product.entity.Product;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.webSocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionNotificationService {

    private final WebSocketService webSocketService;
    private final NotificationQueueService notificationQueueService;
    private final MemberRepository memberRepository;

    // 경매 시작 알림
    public void notifyAuctionStart(Long userId, Product product) {
        String message = String.format("관심 상품 '%s'의 경매가 시작되었습니다! 시작가: %,d원",
            product.getProductName(), product.getInitialPrice());

        Map<String, Object> data = Map.of(
            "type", "AUCTION_START",
            "productId", product.getId(),
            "productName", product.getProductName(),
            "initialPrice", product.getInitialPrice(),
            "startTime", product.getStartTime().toString(),
            "endTime", product.getEndTime().toString()
        );

        webSocketService.sendNotificationToUser(userId.toString(), message, data);

        // DB 큐에도 저장
        Member member = memberRepository.findById(userId).orElse(null);
        if (member != null) {
            notificationQueueService.enqueueNotification(member, message, "AUCTION_START", product);
        }

        log.info("경매 시작 알림 전송 - 사용자: {}, 상품: {}", userId, product.getId());
    }

    // 경매 곧 종료 알림 (10분 전)
    public void notifyAuctionEndingSoon(Long userId, Product product, long remainingMinutes) {
        String message = String.format("'%s' 경매가 %d분 후 종료됩니다! 현재가: %,d원",
            product.getProductName(), remainingMinutes, product.getCurrentPrice());

        Map<String, Object> data = Map.of(
            "type", "AUCTION_ENDING_SOON",
            "productId", product.getId(),
            "productName", product.getProductName(),
            "currentPrice", product.getCurrentPrice(),
            "remainingMinutes", remainingMinutes,
            "endTime", product.getEndTime().toString()
        );

        webSocketService.sendNotificationToUser(userId.toString(), message, data);

        // DB 큐에도 저장
        Member member = memberRepository.findById(userId).orElse(null);
        if (member != null) {
            notificationQueueService.enqueueNotification(member, message, "AUCTION_ENDING_SOON", product);
        }

        log.info("경매 종료 임박 알림 전송 - 사용자: {}, 상품: {}, 남은 시간: {}분", userId, product.getId(), remainingMinutes);
    }

    // 경매 종료 알림
    public void notifyAuctionEnd(Long userId, Product product, boolean hasWinner, Long finalPrice) {
        String message = hasWinner ?
            String.format("'%s' 경매가 종료되었습니다. 최종 낙찰가: %,d원", product.getProductName(), finalPrice) :
            String.format("'%s' 경매가 종료되었습니다. 입찰이 없어 유찰되었습니다.", product.getProductName());

        Map<String, Object> data = Map.of(
            "type", "AUCTION_END",
            "productId", product.getId(),
            "productName", product.getProductName(),
            "hasWinner", hasWinner,
            "finalPrice", finalPrice,
            "status", hasWinner ? "낙찰" : "유찰"
        );

        webSocketService.sendNotificationToUser(userId.toString(), message, data);

        // DB 큐에도 저장
        Member member = memberRepository.findById(userId).orElse(null);
        if (member != null) {
            notificationQueueService.enqueueNotification(member, message, "AUCTION_END", product);
        }

        log.info("경매 종료 알림 전송 - 사용자: {}, 상품: {}, 결과: {}", userId, product.getId(), hasWinner ? "낙찰" : "유찰");
    }
}
