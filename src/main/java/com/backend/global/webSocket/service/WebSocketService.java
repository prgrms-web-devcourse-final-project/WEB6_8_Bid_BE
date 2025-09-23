package com.backend.global.webSocket.service;

import com.backend.global.webSocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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



}
