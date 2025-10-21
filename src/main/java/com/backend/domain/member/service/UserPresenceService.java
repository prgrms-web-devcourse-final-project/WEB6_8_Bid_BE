package com.backend.domain.member.service;

import com.backend.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 온라인/오프라인 상태 관리 서비스
 * WebSocket 연결 상태와 Redis를 활용하여 사용자의 실시간 접속 상태를 추적
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {
    
    private final RedisUtil redisUtil;
    
    // WebSocket 세션과 사용자 이메일 매핑 (메모리 캐시)
    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    // 온라인 사용자 키 접두사
    private static final String ONLINE_USER_KEY_PREFIX = "online:user:";
    private static final String ONLINE_USERS_SET_KEY = "online:users";
    
    // 온라인 상태 TTL (5분 - 주기적으로 갱신됨)
    private static final Duration ONLINE_STATUS_TTL = Duration.ofMinutes(5);
    
    /**
     * 사용자를 온라인 상태로 설정
     */
    public void setUserOnline(String userEmail, String sessionId) {
        try {
            // Redis에 온라인 상태 저장
            String userKey = ONLINE_USER_KEY_PREFIX + userEmail;
            redisUtil.setDataExpire(userKey, sessionId, ONLINE_STATUS_TTL.toSeconds());
            
            // 온라인 사용자 Set에 추가
            redisUtil.setSAdd(ONLINE_USERS_SET_KEY, userEmail);
            
            // 메모리 캐시에 저장
            sessionUserMap.put(sessionId, userEmail);
            
            log.info("사용자 온라인 상태 설정: {} (세션: {})", userEmail, sessionId);
        } catch (Exception e) {
            log.error("사용자 온라인 상태 설정 실패: {}", userEmail, e);
        }
    }
    
    /**
     * 사용자를 오프라인 상태로 설정
     */
    public void setUserOffline(String sessionId) {
        try {
            String userEmail = sessionUserMap.remove(sessionId);
            
            if (userEmail != null) {
                // 다른 세션이 있는지 확인
                boolean hasOtherSessions = sessionUserMap.values().stream()
                    .anyMatch(email -> email.equals(userEmail));
                
                if (!hasOtherSessions) {
                    // Redis에서 온라인 상태 제거
                    String userKey = ONLINE_USER_KEY_PREFIX + userEmail;
                    redisUtil.deleteData(userKey);
                    
                    // 온라인 사용자 Set에서 제거
                    redisUtil.setSRem(ONLINE_USERS_SET_KEY, userEmail);
                    
                    log.info("사용자 오프라인 상태 설정: {} (세션: {})", userEmail, sessionId);
                } else {
                    log.debug("사용자 {}의 다른 세션이 존재함", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("사용자 오프라인 상태 설정 실패: 세션 {}", sessionId, e);
        }
    }
    
    /**
     * 사용자가 온라인인지 확인
     */
    public boolean isUserOnline(String userEmail) {
        try {
            String userKey = ONLINE_USER_KEY_PREFIX + userEmail;
            return redisUtil.existData(userKey);
        } catch (Exception e) {
            log.error("사용자 온라인 상태 확인 실패: {}", userEmail, e);
            return false;
        }
    }
    
    /**
     * 사용자 ID로 온라인 상태 확인
     */
    public boolean isUserOnlineById(Long userId) {
        // 이 메서드는 MemberRepository를 주입받아 이메일을 조회하는 로직이 필요
        // 순환 참조를 피하기 위해 별도 처리 필요
        return false;
    }
    
    /**
     * 온라인 사용자 목록 조회
     */
    public Set<Object> getOnlineUsers() {
        try {
            return redisUtil.setSMembers(ONLINE_USERS_SET_KEY);
        } catch (Exception e) {
            log.error("온라인 사용자 목록 조회 실패", e);
            return Set.of();
        }
    }
    
    /**
     * 사용자의 온라인 상태 갱신 (Heartbeat)
     */
    public void refreshUserOnlineStatus(String userEmail) {
        try {
            if (isUserOnline(userEmail)) {
                String userKey = ONLINE_USER_KEY_PREFIX + userEmail;
                redisUtil.setDataExpire(userKey, "active", ONLINE_STATUS_TTL.toSeconds());
                log.debug("사용자 온라인 상태 갱신: {}", userEmail);
            }
        } catch (Exception e) {
            log.error("사용자 온라인 상태 갱신 실패: {}", userEmail, e);
        }
    }
    
    /**
     * 세션 ID로 사용자 이메일 조회
     */
    public String getUserEmailBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
    
    /**
     * 모든 온라인 상태 초기화 (서버 재시작 시)
     */
    public void clearAllOnlineStatus() {
        try {
            Set<Object> onlineUsers = getOnlineUsers();
            for (Object userEmail : onlineUsers) {
                String userKey = ONLINE_USER_KEY_PREFIX + userEmail;
                redisUtil.deleteData(userKey);
            }
            redisUtil.deleteData(ONLINE_USERS_SET_KEY);
            sessionUserMap.clear();
            
            log.info("모든 온라인 상태 초기화 완료");
        } catch (Exception e) {
            log.error("온라인 상태 초기화 실패", e);
        }
    }
}
