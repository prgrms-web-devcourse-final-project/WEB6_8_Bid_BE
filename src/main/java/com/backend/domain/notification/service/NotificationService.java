package com.backend.domain.notification.service;

import com.backend.domain.notification.dto.NotificationListResponseDto;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import com.backend.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public RsData<NotificationListResponseDto> getNotifications(Long memberId, int page, int size, Boolean isRead) {
        Pageable pageable = PageRequest.of(page,size);
        // 1. 조건에 따른 알림조회
        Page<Notification> notificationPage;
        if (isRead != null && !isRead) {
            // 읽지 않은 알림만 조회
            notificationPage = notificationRepository.findUnreadNotifications(memberId, pageable);
        } else if (isRead != null && isRead) {
            // 읽은 알림만 조회
            notificationPage = notificationRepository.findReadNotifications(memberId, pageable);
        } else {
            // 전체 알림 조회 (isRead가 null인 경우)
            notificationPage = notificationRepository.findByMemberId(memberId, pageable);
        }
        // 2. 읽지않은 알림 개수 조회
        Integer unreadCount = notificationRepository.countUnreadNotifications(memberId);
        // 3. DTO 변환 및 response 반환
        List<NotificationListResponseDto.NotificationItem> notificationItems = notificationPage.getContent().stream()
                .map(this::convertToNotifiactionItem).toList();
        NotificationListResponseDto response = new NotificationListResponseDto(
                notificationItems,
                (int) notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.hasNext(),
                unreadCount
        );
        return new RsData<>("200","알림 목록 조회가 완료되었습니다.",response);
    }

    public RsData<Void> markAsRead(Long notificationId, Long memberId) {
        // 1. 알림 존재 및 사용자 확인
        Optional<Notification> notification = notificationRepository.findByIdAndMemberId(notificationId,memberId);
        if (notification.isEmpty()) return new RsData<>("404","알림을 찾을 수 없습니다.",null);
        // 2. 읽음 처리
        Notification n = notification.get();
        n.setIsRead(true);
        notificationRepository.save(n);
        return new RsData<>("200","알림이 읽음 처리되었습니다.",null);
    }

    @Transactional
    public RsData<Integer> markAllAsRead(Long memberId) {
        int updatedCount =  notificationRepository.markAllAsRead(memberId);
        return new RsData<>("200","모든 알림이 읽음 처리되었습니다.",updatedCount);
    }

    public RsData<Integer> getUnreadCount(Long memberId) {
        Integer unreadCount = notificationRepository.countUnreadNotifications(memberId);
        return new RsData<>("200","읽지 않은 알림 개수가 조회되었습니다.",unreadCount);
    }

    private NotificationListResponseDto.NotificationItem convertToNotifiactionItem(Notification notification) {
        String productName = null;
        Long productId = null;
        if(notification.getProduct()!=null){
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
                notification.getCreateDate()
        );
    }
}
