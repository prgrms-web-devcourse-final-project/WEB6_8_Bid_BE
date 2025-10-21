package com.backend.domain.notification.service;

import com.backend.domain.member.service.UserPresenceService;
import com.backend.domain.notification.entity.Notification;
import com.backend.global.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 알림 처리기
 * 사용자의 온라인/오프라인 상태에 따라 적절한 채널로 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessor {
    
    private final UserPresenceService userPresenceService;
    private final WebSocketService webSocketService;

    /**
     * 알림 처리 - 온라인/오프라인 상태에 따라 적절한 채널로 전송
     */
    public boolean processNotification(Notification notification) {
        try {
            String userEmail = notification.getMember().getEmail();
            String message = notification.getMessage();

            log.debug("알림 처리 시작: type={}, member_id={}, email={}",
                    notification.getNotificationType(),
                    notification.getMember().getId(),
                    userEmail);

            // 알림 데이터 구성
            Map<String, Object> notificationData = Map.of(
                    "notificationId", notification.getId(),
                    "type", notification.getNotificationType(),
                    "productId", notification.getProduct() != null ? notification.getProduct().getId() : "",
                    "productName", notification.getProduct() != null ? notification.getProduct().getProductName() : "",
                    "createdAt", notification.getCreateDate()
            );

            boolean isOnline = userPresenceService.isUserOnline(userEmail);

            if (isOnline) {
                // 온라인 상태: WebSocket으로 실시간 알림 전송
                try {
                    webSocketService.sendNotificationToUser(userEmail, message, notificationData);
                    log.info("온라인 알림 전송 성공 - 사용자: {}, 타입: {}",
                            userEmail, notification.getNotificationType());
                    return true;
                } catch (Exception e) {
                    log.error("WebSocket 알림 전송 실패 - 사용자: {}", userEmail, e);
                    return false;
                }
            } else {
                // 오프라인 상태: 알림을 보내지 않음
                log.info("사용자 오프라인 - 알림을 보내지 않음: {}", userEmail);
                return true;
            }

        } catch (Exception e) {
            log.error("알림 처리 중 예외 발생: notification_id={}", notification.getId(), e);
            return false;
        }
    }
}
