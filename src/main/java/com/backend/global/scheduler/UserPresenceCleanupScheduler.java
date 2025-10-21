package com.backend.global.scheduler;

import com.backend.domain.member.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 사용자 온라인 상태 정리 스케줄러
 * 만료된 온라인 상태를 주기적으로 정리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserPresenceCleanupScheduler {
    
    private final UserPresenceService userPresenceService;
    
    /**
     * 애플리케이션 시작 시 모든 온라인 상태 초기화
     * 서버 재시작 시 이전 세션 정보를 정리
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("애플리케이션 시작 - 사용자 온라인 상태 초기화");
        userPresenceService.clearAllOnlineStatus();
    }
    
    /**
     * 주기적으로 온라인 사용자 수 로깅 (모니터링용)
     * 10분마다 실행
     */
    @Scheduled(fixedDelay = 600000) // 10분
    public void logOnlineUsersCount() {
        try {
            int onlineCount = userPresenceService.getOnlineUsers().size();
            log.info("현재 온라인 사용자 수: {}", onlineCount);
        } catch (Exception e) {
            log.error("온라인 사용자 수 조회 실패", e);
        }
    }
}
