package com.backend.domain.notification.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.dto.NotificationListResponseDto;
import com.backend.domain.notification.service.NotificationService;
import com.backend.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 관련 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class ApiV1NotificationController {
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이지네이션으로 조회, 읽음 상태로 필터링 가능.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @GetMapping
    public RsData<NotificationListResponseDto> getNotifications(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "읽음 상태 필터 (true: 읽음, false: 안읽음, null: 전체)", example = "false") @RequestParam(required = false) Boolean isRead,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        Long memberId = getCurrentMemberId(user);
        return notificationService.getNotifications(memberId, page, size, isRead);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @PutMapping("/{id}/read")
    public RsData<Void> markAsRead(
            @Parameter(description = "알림 ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        Long memberId = getCurrentMemberId(user);
        return notificationService.markAsRead(id, memberId);
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "모든 알림 읽음 처리 성공",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @PutMapping("/read-all")
    public RsData<Integer> markAllAsRead(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        Long memberId = getCurrentMemberId(user);
        return notificationService.markAllAsRead(memberId);
    }

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "사용자의 읽지 않은 알림 개수 조회.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "읽지 않은 알림 개수 조회 성공",
            content = @Content(schema = @Schema(implementation = RsData.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    @GetMapping("/unread-count")
    public RsData<Integer> getUnreadCount(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        Long memberId = getCurrentMemberId(user);
        return notificationService.getUnreadCount(memberId);
    }

    private Long getCurrentMemberId(User user) {
        if (user != null) {
            return Long.parseLong(user.getUsername());
        } else {
            // 테스트용: 인증이 없으면 첫 번째 사용자 사용
            Member member = memberRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("멤버가 존재하지 않습니다."));
            return member.getId();
        }
    }
}
