package com.backend.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    private MessageType type;
    private String sender;
    private String content;
    private Object data;
    private LocalDateTime timestamp;
    
    public enum MessageType {
        CHAT,           // 메시지
        BID,            // 입찰 정보
        AUCTION_TIMER,  // 경매 타이머
        NOTIFICATION,   // 알림
        SYSTEM          // 시스템 메시지
    }
    
    public static WebSocketMessage of(MessageType type, String sender, String content, Object data) {
        return WebSocketMessage.builder()
                .type(type)
                .sender(sender)
                .content(content)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
