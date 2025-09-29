package com.backend.domain.notification.repository;

import com.backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 사용자 알림 목록 조회
    @Query("SELECT n FROM Notification n WHERE n.member.id = :memberId ORDER BY n.createDate DESC")
    Page<Notification> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);
    // 읽지 않은 알람만 조회
    @Query("SELECT n FROM Notification n WHERE n.member.id = :memberId AND n.isRead = false ORDER BY n.createDate DESC")
    Page<Notification> findUnreadNotifications(@Param("memberId") Long memberId, Pageable pageable);
    // 읽은 알람만 조회
    @Query("SELECT n FROM Notification n WHERE n.member.id = :memberId AND n.isRead = true ORDER BY n.createDate DESC")
    Page<Notification> findReadNotifications(@Param("memberId") Long memberId, Pageable pageable);
    // 읽지 않은 알람 개수 조회
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.member.id = :memberId AND n.isRead = false")
    Integer countUnreadNotifications(@Param("memberId") Long memberId);
    // 특정 알림 읽음 처리를 위한 조회
    @Query("SELECT n FROM Notification n WHERE n.id = :notificationId AND n.member.id = :memberId")
    Optional<Notification> findByIdAndMemberId(@Param("notificationId") Long notificationId, @Param("memberId") Long memberId);
    // 모든 읽지않은 알람 읽음처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.member.id = :memberId AND n.isRead = false")
    int markAllAsRead(@Param("memberId") Long memberId);

    // 처리 대기중인 알림들 조회 (스케줄된 시간이 지났거나 즉시 처리할 것들)
    @Query("SELECT n FROM Notification n WHERE n.queueStatus = 'PENDING' " +
           "AND (n.scheduledTime IS NULL OR n.scheduledTime <= :now) " +
           "ORDER BY n.createDate ASC")
    List<Notification> findPendingNotifications(@Param("now") LocalDateTime now, Pageable pageable);

    // 실패한 알림 중 재시도 가능한 것들 조회
    @Query("SELECT n FROM Notification n WHERE n.queueStatus = 'FAILED' " +
           "AND n.retryCount < 3 " +
           "ORDER BY n.createDate ASC")
    List<Notification> findRetryableFailedNotifications(Pageable pageable);

    // 처리중인 상태로 오래 머물러 있는 알림들 (5분 이상)
    @Query("SELECT n FROM Notification n WHERE n.queueStatus = 'PROCESSING' " +
           "AND n.modifyDate < :timeThreshold")
    List<Notification> findStuckProcessingNotifications(@Param("timeThreshold") LocalDateTime timeThreshold);

    // 큐 상태별 개수 조회
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.queueStatus = :status")
    Long countByQueueStatus(@Param("status") Notification.QueueStatus status);
}
