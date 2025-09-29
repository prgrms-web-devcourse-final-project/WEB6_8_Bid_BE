package com.backend.domain.notification.dto;

import com.backend.domain.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationListResponseDto (
        List<NotificationItem> content,
        Integer totalElements,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize,
        Boolean hasNext,
        Integer unreadCount
) {
    public record NotificationItem (
            Long id,
            String message,
            String type,
            Boolean isRead,
            Long productId,
            String productName,
            LocalDateTime createDate,
            Notification.QueueStatus queueStatus,
            Integer retryCount,
            LocalDateTime scheduledTime,
            LocalDateTime sentTime,
            String errorMessage
    ) {}
}
