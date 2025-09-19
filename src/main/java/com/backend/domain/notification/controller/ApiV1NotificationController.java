package com.backend.domain.notification.controller;

import com.backend.domain.notification.dto.NotificationListResponseDto;
import com.backend.domain.notification.service.NotificationService;
import com.backend.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class ApiV1NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public RsData<NotificationListResponseDto> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead
    ) {
        // TODO: JWT 토큰에서 사용자 id 추출
        Long memberId = 1L;
        return notificationService.getNotifications(memberId, page, size, isRead);
    }
}
