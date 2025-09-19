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

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public RsData<NotificationListResponseDto> getNotifications(Long memberId, int page, int size, Boolean isRead) {
        Pageable pageable = PageRequest.of(page,size);
        // 1. 조건에 따른 알림조회
        Page<Notification> notificationPage;
        if (isRead!=null&&!isRead) notificationPage = notificationRepository.findUnreadNotifications(memberId,pageable);
        else notificationPage = notificationRepository.findByMemberId(memberId,pageable);
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
