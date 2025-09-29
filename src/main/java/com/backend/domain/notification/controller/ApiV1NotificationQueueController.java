package com.backend.domain.notification.controller;

import com.backend.domain.notification.service.NotificationQueueService;
import com.backend.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification-queue")
@RequiredArgsConstructor
public class ApiV1NotificationQueueController {
    
    private final NotificationQueueService notificationQueueService;
    
    // 큐 상태 조회 (개발용)
    @GetMapping("/status")
    public RsData<NotificationQueueService.QueueStatus> getQueueStatus() {
        NotificationQueueService.QueueStatus status = notificationQueueService.getQueueStatus();
        return new RsData<>("200", "큐 상태 조회 완료", status);
    }
}
