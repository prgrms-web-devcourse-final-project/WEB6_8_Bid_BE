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



}
