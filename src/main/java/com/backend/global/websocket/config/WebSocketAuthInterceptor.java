package com.backend.global.websocket.config;

import com.backend.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // STOMP CONNECT 시 Authorization 헤더에서 JWT 토큰 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    if (jwtUtil.validateToken(token)) {
                        String email = jwtUtil.getEmailFromToken(token);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                        
                        // WebSocket 세션에 사용자 정보 설정
                        accessor.setUser(authentication);
                        
                        log.info("WebSocket 인증 성공: {}", email);
                    }
                } catch (Exception e) {
                    log.error("WebSocket 인증 실패: {}", e.getMessage());
                }
            }
        }

        return message;
    }
}
