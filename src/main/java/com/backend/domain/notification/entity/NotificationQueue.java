package com.backend.domain.notification.entity;

import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_queue")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationQueue extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String type; // BID_SUCCESS, BID_OUTBID, AUCTION_WON
    
    @Column(nullable = false, length = 500)
    private String message;
    
    @Column(columnDefinition = "TEXT")
    private String data;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    private LocalDateTime scheduledAt; // 발송 예정 시간
    private LocalDateTime sentAt; // 실제 발송 시간
    private LocalDateTime failedAt; // 실패 시간
    
    @Column(nullable = false)
    private int retryCount = 0; // 재시도 횟수
    
    private String failureReason; // 실패 사유
    
    public enum NotificationStatus {
        PENDING,    // 대기 중
        SENT,       // 발송 완료
        FAILED,     // 발송 실패
        CANCELLED   // 취소됨
    }
    
    // 재시도 가능 여부 확인
    public boolean canRetry() {
        return retryCount < 3 && status == NotificationStatus.FAILED;
    }
    
    // 재시도 횟수 증가
    public void incrementRetryCount() {
        this.retryCount++;
    }
}
