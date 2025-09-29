package com.backend.domain.notification.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String message;

    @Column(name = "notification_type",nullable = false,length = 50)
    private String notificationType;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false, length = 20)
    private QueueStatus queueStatus = QueueStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public enum QueueStatus {
        PENDING,    // 대기중
        PROCESSING, // 처리중
        SENT,       // 발송완료
        FAILED      // 실패
    }

    public void markAsSent() {
        this.queueStatus = QueueStatus.SENT;
        this.sentTime = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.queueStatus = QueueStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    public void markAsProcessing() {
        this.queueStatus = QueueStatus.PROCESSING;
    }

    public void resetToPending() {
        this.queueStatus = QueueStatus.PENDING;
    }

    public boolean canRetry() {
        return this.retryCount < 3; // 최대 3번 재시도 후 FAILED
    }
}