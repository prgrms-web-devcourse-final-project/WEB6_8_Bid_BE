package com.backend.global.websocket.controller;

import com.backend.global.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public WebSocketMessage handleTestMessage(WebSocketMessage message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("WebSocket 테스트 메시지 수신: {}", message);
        
        return WebSocketMessage.of(
                WebSocketMessage.MessageType.SYSTEM,
                "server",
                "메시지가 성공적으로 처리되었습니다: " + message.getContent(),
                null
        );
    }
}
