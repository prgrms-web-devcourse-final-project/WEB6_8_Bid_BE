package com.backend.global.websocket.controller;

import com.backend.domain.member.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket Heartbeat 컨트롤러
 * 클라이언트의 연결 상태를 주기적으로 확인
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class HeartbeatController {
    
    private final UserPresenceService userPresenceService;
    
    /**
     * 클라이언트로부터 heartbeat 수신 및 응답
     * 클라이언트는 주기적으로 이 엔드포인트로 메시지를 전송하여 연결 상태를 유지
     */
    @MessageMapping("/heartbeat")
    @SendToUser("/queue/heartbeat")
    public Map<String, Object> handleHeartbeat(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String userEmail = authentication.getName();
            
            // 사용자 온라인 상태 갱신
            userPresenceService.refreshUserOnlineStatus(userEmail);
            
            log.debug("Heartbeat 수신 - 사용자: {}", userEmail);
            
            return Map.of(
                "status", "alive",
                "timestamp", LocalDateTime.now().toString(),
                "user", userEmail
            );
        }
        
        return Map.of(
            "status", "unauthorized",
            "timestamp", LocalDateTime.now().toString()
        );
    }
    
    /**
     * 전체 heartbeat 브로드캐스트 (서버 상태 확인용)
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> handlePing() {
        return Map.of(
            "status", "pong",
            "serverTime", LocalDateTime.now().toString()
        );
    }
}
