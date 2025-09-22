package com.backend.domain.notification.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.dto.NotificationListResponseDto;
import com.backend.domain.notification.service.NotificationService;
import com.backend.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class ApiV1NotificationController {
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @GetMapping
    public RsData<NotificationListResponseDto> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead
    ) {
        Long memberId = getCurrentMemberId();
        return notificationService.getNotifications(memberId, page, size, isRead);
    }

    @PutMapping("/{id}/read")
    public RsData<Void> markAsRead(@PathVariable Long id) {
        Long memberId = getCurrentMemberId();
        return notificationService.markAsRead(id, memberId);
    }

    @PutMapping("/read-all")
    public RsData<Integer> markAllAsRead() {
        Long memberId = getCurrentMemberId();
        return notificationService.markAllAsRead(memberId);
    }

    @GetMapping("/unread-count")
    public RsData<Integer> getUnreadCount() {
        Long memberId = getCurrentMemberId();
        return notificationService.getUnreadCount(memberId);
    }

    private Long getCurrentMemberId() {
        Member member = memberRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("멤버가 존재하지 않습니다."));
        return member.getId();
    }
}
