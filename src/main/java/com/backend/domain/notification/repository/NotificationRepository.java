package com.backend.domain.notification.repository;

import com.backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 사용자 알림 목록 조회
    @Query("SELECT n FROM Notification n WHERE n.member.id = :memberId ORDER BY n.createDate DESC")
    Page<Notification> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);
    // 특정 알림 읽음 처리를 위한 조회
    @Query("SELECT n FROM Notification n WHERE n.id = :notificationId AND n.member.id = :memberId")
    Optional<Notification> findByIdAndMemberId(@Param("notificationId") Long notificationId, @Param("memberId") Long memberId);
}
