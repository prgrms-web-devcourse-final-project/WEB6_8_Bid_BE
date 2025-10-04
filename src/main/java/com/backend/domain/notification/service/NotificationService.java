package com.backend.domain.notification.service;

import com.backend.domain.notification.dto.NotificationListResponseDto;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import com.backend.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    // ======================================= find/get methods ======================================= //
    @Transactional(readOnly = true)
    public RsData<NotificationListResponseDto> getNotifications(Long memberId, int page, int size, Boolean isRead) {
        // 페이지 설정
        Pageable pageable = PageRequest.of(page, size);

        // 조건에 따른 알림 조회
        Page<Notification> notificationPage = getNotificationsByReadStatus(memberId, isRead, pageable);

        // 읽지 않은 알림 개수 조회
        Integer unreadCount = notificationRepository.countUnreadNotifications(memberId);

        // 응답 데이터 생성
        List<NotificationListResponseDto.NotificationItem> notificationItems = notificationPage.getContent().stream()
                .map(this::convertToNotificationItem)
                .toList();

        NotificationListResponseDto response = new NotificationListResponseDto(
                notificationItems,
                (int) notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.hasNext(),
                unreadCount
        );

        return RsData.ok("알림 목록 조회가 완료되었습니다.", response);
    }

    @Transactional(readOnly = true)
    public RsData<Integer> getUnreadCount(Long memberId) {
        Integer unreadCount = notificationRepository.countUnreadNotifications(memberId);
        return RsData.ok("읽지 않은 알림 개수가 조회되었습니다.", unreadCount);
    }

    private Page<Notification> getNotificationsByReadStatus(Long memberId, Boolean isRead, Pageable pageable) {
        if (isRead != null && !isRead) {
            // 읽지 않은 알림만 조회
            return notificationRepository.findUnreadNotifications(memberId, pageable);
        } else if (isRead != null && isRead) {
            // 읽은 알림만 조회
            return notificationRepository.findReadNotifications(memberId, pageable);
        } else {
            // 전체 알림 조회
            return notificationRepository.findByMemberId(memberId, pageable);
        }
    }

    // ======================================= update methods ======================================= //
    @Transactional
    public RsData<Void> markAsRead(Long notificationId, Long memberId) {
        // 알림 존재 및 사용자 확인
        Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> com.backend.global.exception.ServiceException.notFound("알림을 찾을 수 없습니다."));

        // 읽음 처리
        notification.setIsRead(true);
        notificationRepository.save(notification);

        return RsData.ok("알림이 읽음 처리되었습니다.", null);
    }

    @Transactional
    public RsData<Integer> markAllAsRead(Long memberId) {
        int updatedCount = notificationRepository.markAllAsRead(memberId);
        return RsData.ok("모든 알림이 읽음 처리되었습니다.", updatedCount);
    }

    // ======================================= helper methods ======================================= //
    private NotificationListResponseDto.NotificationItem convertToNotificationItem(Notification notification) {
        String productName = null;
        Long productId = null;

        if (notification.getProduct() != null) {
            Product product = notification.getProduct();
            productName = product.getProductName();
            productId = product.getId();
        }

        return new NotificationListResponseDto.NotificationItem(
                notification.getId(),
                notification.getMessage(),
                notification.getNotificationType(),
                notification.getIsRead(),
                productId,
                productName,
                notification.getCreateDate(),
                notification.getQueueStatus(),
                notification.getRetryCount(),
                notification.getScheduledTime(),
                notification.getSentTime(),
                notification.getErrorMessage()
        );
    }
}
