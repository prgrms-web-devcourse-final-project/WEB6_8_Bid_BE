package com.backend.global.webSocket.service;

import com.backend.global.webSocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 특정 토픽에 메시지 브로드캐스트
     */
    public void sendToTopic(String topic, WebSocketMessage message) {
        log.info("토픽 {} 에 메시지 전송: {}", topic, message);
        messagingTemplate.convertAndSend("/topic/" + topic, message);
    }

    /**
     * 입찰 정보 브로드캐스트 (상품별)
     */
    public void broadcastBidUpdate(Long productId, Object bidData) {
        WebSocketMessage message = WebSocketMessage.of(
                WebSocketMessage.MessageType.BID,
                "system",
                "새로운 입찰이 등록되었습니다.",
                bidData
        );
        sendToTopic("bid/" + productId, message);
    }

    /**
     * 경매 종료 임박 알림 브로드캐스트
     */
    public void broadcastAuctionEndingSoon(Long productId, String productName) {
        String content = String.format("'%s' 경매가 10분 후 종료됩니다!", productName);
            
        Object data = Map.of(
            "productId", productId,
            "productName", productName,
            "remainingMinutes", 10
        );
        
        WebSocketMessage message = WebSocketMessage.of(
                WebSocketMessage.MessageType.SYSTEM,
                "system",
                content,
                data
        );
        sendToTopic("bid/" + productId, message);
    }

    /**
     * 개인 알림 전송 (특정 사용자)
     */
    public void sendNotificationToUser(String userId, String message, Object data) {
        WebSocketMessage webSocketMessage = WebSocketMessage.of(
                WebSocketMessage.MessageType.NOTIFICATION,
                "system",
                message,
                data
        );
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", webSocketMessage);
        log.info("개인 알림 전송 - 사용자: {}, 메시지: {}", userId, message);
    }

    /**
     * 경매 종료 알림 브로드캐스트
     */
    public void broadcastAuctionEnd(Long productId, boolean isSuccessful, Long finalPrice) {
        String content = isSuccessful ? 
            "경매가 종료되었습니다! 낙찰가: " + finalPrice.toString() + "원" : 
            "경매가 종료되었습니다. 입찰이 없어 유찰되었습니다.";
            
        Object data = Map.of(
            "productId", productId,
            "isSuccessful", isSuccessful,
            "finalPrice", finalPrice,
            "status", isSuccessful ? "낙찰" : "유찰"
        );
        
        WebSocketMessage message = WebSocketMessage.of(
                WebSocketMessage.MessageType.SYSTEM,
                "system",
                content,
                data
        );
        sendToTopic("bid/" + productId, message);
    }

    /**
     * 경매 시작 알림 브로드캐스트
     */
    public void broadcastAuctionStart(Long productId, String productName) {
        String content = String.format("'%s' 경매가 시작되었습니다!", productName);

        Object data = Map.of(
                "productId", productId,
                "productName", productName,
                "status", "경매 중"
        );

        WebSocketMessage message = WebSocketMessage.of(
                WebSocketMessage.MessageType.SYSTEM,
                "system",
                content,
                data
        );
        sendToTopic("bid/" + productId, message);
    }
}
