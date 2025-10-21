package com.backend.global.websocket.listener;

import com.backend.domain.member.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * WebSocket 세션 이벤트 리스너
 * 사용자의 WebSocket 연결/해제를 감지하고 온라인 상태를 관리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    
    private final UserPresenceService userPresenceService;
    
    /**
     * WebSocket 연결 이벤트 처리
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        if (headerAccessor.getUser() != null && headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken authentication = 
                (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
            
            String userEmail = authentication.getName();
            String sessionId = headerAccessor.getSessionId();
            
            // 사용자를 온라인 상태로 설정
            userPresenceService.setUserOnline(userEmail, sessionId);
            
            log.info("WebSocket 연결 - 사용자: {}, 세션: {}", userEmail, sessionId);
        }
    }
    
    /**
     * WebSocket 연결 해제 이벤트 처리
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 사용자를 오프라인 상태로 설정
        userPresenceService.setUserOffline(sessionId);
        
        log.info("WebSocket 연결 해제 - 세션: {}", sessionId);
    }
    
    /**
     * WebSocket 구독 이벤트 처리 (옵션)
     * 특정 토픽 구독 시 추가 처리가 필요한 경우 사용
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        if (headerAccessor.getUser() != null) {
            String destination = headerAccessor.getDestination();
            String userEmail = headerAccessor.getUser().getName();
            
            log.debug("WebSocket 구독 - 사용자: {}, 대상: {}", userEmail, destination);
            
            // 사용자 온라인 상태 갱신 (Heartbeat 역할)
            userPresenceService.refreshUserOnlineStatus(userEmail);
        }
    }
}
