package com.backend.domain.notification.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationQueueService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationProcessor notificationProcessor;
    
    // 알림을 큐에 추가 (즉시 처리)
    public Notification enqueueNotification(Member member, String message, String notificationType, Product product) {
        Notification notification = new Notification();
        notification.setMember(member);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setProduct(product);
        notification.setQueueStatus(Notification.QueueStatus.PENDING);
        notification.setRetryCount(0);
        // scheduledTime이 null이면 즉시 처리
        
        return notificationRepository.save(notification);
    }
    
    // 알림을 큐에 추가 (예약 발송)
    public Notification enqueueScheduledNotification(Member member, String message, String notificationType, 
                                                   Product product, LocalDateTime scheduledTime) {
        Notification notification = new Notification();
        notification.setMember(member);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setProduct(product);
        notification.setQueueStatus(Notification.QueueStatus.PENDING);
        notification.setRetryCount(0);
        notification.setScheduledTime(scheduledTime);
        
        return notificationRepository.save(notification);
    }
    
    // 큐 처리 스케줄러 - 매 30초마다 실행
    @Scheduled(fixedDelay = 30000) // 30초마다
    public void processNotificationQueue() {
        log.info("알림 큐 처리 시작");
        
        // 1. 대기중인 알림들 처리
        processPendingNotifications();
        
        // 2. 실패한 알림들 재시도
        retryFailedNotifications();
        
        // 3. 처리중 상태로 오래 머물러 있는 알림들 복구
        recoverStuckNotifications();
        
        log.info("알림 큐 처리 완료");
    }
    
    private void processPendingNotifications() {
        Pageable pageable = PageRequest.of(0, 50); // 한 번에 50개씩 처리
        List<Notification> pendingNotifications = notificationRepository.findPendingNotifications(
            LocalDateTime.now(), pageable);
        
        for (Notification notification : pendingNotifications) {
            processNotificationAsync(notification);
        }
        
        log.info("대기중인 알림 {}개 처리 시작", pendingNotifications.size());
    }
    
    private void retryFailedNotifications() {
        Pageable pageable = PageRequest.of(0, 10); // 재시도는 더 적게
        List<Notification> retryableNotifications = notificationRepository.findRetryableFailedNotifications(pageable);
        
        for (Notification notification : retryableNotifications) {
            notification.resetToPending();
            notificationRepository.save(notification);
            processNotificationAsync(notification);
        }
        
        if (!retryableNotifications.isEmpty()) {
            log.info("실패한 알림 {}개 재시도", retryableNotifications.size());
        }
    }
    
    private void recoverStuckNotifications() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Notification> stuckNotifications = notificationRepository.findStuckProcessingNotifications(fiveMinutesAgo);
        
        for (Notification notification : stuckNotifications) {
            notification.resetToPending();
            notificationRepository.save(notification);
        }
        
        if (!stuckNotifications.isEmpty()) {
            log.warn("처리중 상태로 멈춰있던 알림 {}개 복구", stuckNotifications.size());
        }
    }
    
    @Async
    public void processNotificationAsync(Notification notification) {
        try {
            // 처리 중으로 상태 변경
            notification.markAsProcessing();
            notificationRepository.save(notification);
            
            // 실제 알림 처리
            boolean success = notificationProcessor.processNotification(notification);
            
            if (success) {
                notification.markAsSent();
                log.debug("알림 처리 성공: ID={}", notification.getId());
            } else {
                notification.markAsFailed("처리 실패");
                log.warn("알림 처리 실패: ID={}", notification.getId());
            }
            
        } catch (Exception e) {
            notification.markAsFailed("예외 발생: " + e.getMessage());
            log.error("알림 처리 중 예외 발생: ID={}", notification.getId(), e);
        } finally {
            notificationRepository.save(notification);
        }
    }
    
    // 큐 상태 조회
    @Transactional(readOnly = true)
    public QueueStatus getQueueStatus() {
        return QueueStatus.builder()
            .pendingCount(notificationRepository.countByQueueStatus(Notification.QueueStatus.PENDING))
            .processingCount(notificationRepository.countByQueueStatus(Notification.QueueStatus.PROCESSING))
            .sentCount(notificationRepository.countByQueueStatus(Notification.QueueStatus.SENT))
            .failedCount(notificationRepository.countByQueueStatus(Notification.QueueStatus.FAILED))
            .build();
    }
    
    @lombok.Builder
    @lombok.Getter
    public static class QueueStatus {
        private final Long pendingCount;
        private final Long processingCount;
        private final Long sentCount;
        private final Long failedCount;
    }
}
