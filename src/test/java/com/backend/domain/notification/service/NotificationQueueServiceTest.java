package com.backend.domain.notification.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.backend.domain.product.enums.DeliveryMethod.TRADE;
import static com.backend.domain.product.enums.ProductCategory.DIGITAL_ELECTRONICS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestElasticsearchConfiguration.class)
class NotificationQueueServiceTest {

    @Autowired
    private NotificationQueueService notificationQueueService;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("즉시 알림 큐 추가 테스트")
    void enqueueNotification() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        String message = "테스트 알림 메시지";
        String notificationType = "BID_SUCCESS";

        // When
        Notification notification = notificationQueueService.enqueueNotification(
                member, message, notificationType, product);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getMessage()).isEqualTo(message);
        assertThat(notification.getNotificationType()).isEqualTo(notificationType);
        assertThat(notification.getQueueStatus()).isEqualTo(Notification.QueueStatus.PENDING);
        assertThat(notification.getRetryCount()).isEqualTo(0);
        assertThat(notification.getScheduledTime()).isNull(); // 즉시 처리
    }

    @Test
    @DisplayName("예약 알림 큐 추가 테스트")
    void enqueueScheduledNotification() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        String message = "예약 알림 메시지";
        String notificationType = "AUCTION_START";
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(10);

        // When
        Notification notification = notificationQueueService.enqueueScheduledNotification(
                member, message, notificationType, product, scheduledTime);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getScheduledTime()).isEqualTo(scheduledTime);
        assertThat(notification.getQueueStatus()).isEqualTo(Notification.QueueStatus.PENDING);
    }

    @Test
    @DisplayName("대기중인 알림 조회 테스트")
    void findPendingNotifications() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        
        // 즉시 처리할 알림 생성
        notificationQueueService.enqueueNotification(member, "즉시 알림", "BID_SUCCESS", product);
        
        // 과거 시간으로 예약된 알림 생성 (처리 대상)
        Notification pastScheduled = notificationQueueService.enqueueScheduledNotification(
                member, "과거 예약 알림", "AUCTION_END", product, LocalDateTime.now().minusMinutes(5));
        
        // 미래 시간으로 예약된 알림 생성 (처리 대상 아님)
        notificationQueueService.enqueueScheduledNotification(
                member, "미래 예약 알림", "AUCTION_START", product, LocalDateTime.now().plusMinutes(10));

        // When
        List<Notification> pendingNotifications = notificationRepository.findPendingNotifications(
                LocalDateTime.now(), PageRequest.of(0, 10));

        // Then
        assertThat(pendingNotifications).hasSize(2); // 즉시 알림 + 과거 예약 알림
        assertThat(pendingNotifications)
                .extracting(Notification::getQueueStatus)
                .containsOnly(Notification.QueueStatus.PENDING);
    }

    @Test
    @DisplayName("큐 상태 조회 테스트")
    void getQueueStatus() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        
        // 다양한 상태의 알림 생성
        Notification pending = notificationQueueService.enqueueNotification(member, "대기중", "BID_SUCCESS", product);
        
        Notification processing = notificationQueueService.enqueueNotification(member, "처리중", "BID_OUTBID", product);
        processing.markAsProcessing();
        notificationRepository.save(processing);
        
        Notification sent = notificationQueueService.enqueueNotification(member, "발송완료", "AUCTION_END", product);
        sent.markAsSent();
        notificationRepository.save(sent);
        
        Notification failed = notificationQueueService.enqueueNotification(member, "실패", "AUCTION_START", product);
        failed.markAsFailed("테스트 에러");
        notificationRepository.save(failed);

        // When
        NotificationQueueService.QueueStatus status = notificationQueueService.getQueueStatus();

        // Then
        assertThat(status.getPendingCount()).isEqualTo(1);
        assertThat(status.getProcessingCount()).isEqualTo(1);
        assertThat(status.getSentCount()).isEqualTo(1);
        assertThat(status.getFailedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("알림 상태 변경 메서드 테스트")
    void notificationStatusMethods() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        Notification notification = notificationQueueService.enqueueNotification(
                member, "상태 테스트", "BID_SUCCESS", product);

        // When & Then - Processing으로 변경
        notification.markAsProcessing();
        assertThat(notification.getQueueStatus()).isEqualTo(Notification.QueueStatus.PROCESSING);

        // When & Then - Sent로 변경
        notification.markAsSent();
        assertThat(notification.getQueueStatus()).isEqualTo(Notification.QueueStatus.SENT);
        assertThat(notification.getSentTime()).isNotNull();

        // When & Then - Pending으로 리셋
        notification.resetToPending();
        assertThat(notification.getQueueStatus()).isEqualTo(Notification.QueueStatus.PENDING);

        // When & Then - Failed로 변경
        String errorMessage = "테스트 에러 메시지";
        notification.markAsFailed(errorMessage);
        assertThat(notification.getQueueStatus()).isEqualTo(Notification.QueueStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(notification.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("재시도 가능 여부 테스트")
    void canRetry() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        Notification notification = notificationQueueService.enqueueNotification(
                member, "재시도 테스트", "BID_SUCCESS", product);

        // When & Then - 초기에는 재시도 가능
        assertThat(notification.canRetry()).isTrue();

        // When & Then - 3번 실패 후에는 재시도 불가
        notification.markAsFailed("에러 1");
        notification.markAsFailed("에러 2");
        notification.markAsFailed("에러 3");
        
        assertThat(notification.getRetryCount()).isEqualTo(3);
        assertThat(notification.canRetry()).isFalse();
    }

    @Test
    @DisplayName("멀티 알림 생성 테스트")
    void createMultipleNotifications() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        int notificationCount = 5;

        // When
        for (int i = 0; i < notificationCount; i++) {
            notificationQueueService.enqueueNotification(
                    member, "테스트 알림 " + (i + 1), "BID_SUCCESS", product);
        }

        // Then
        List<Notification> pendingNotifications = notificationRepository.findPendingNotifications(
                LocalDateTime.now(), PageRequest.of(0, 10));
        
        assertThat(pendingNotifications).hasSize(notificationCount);
        assertThat(pendingNotifications)
                .extracting(Notification::getMessage)
                .contains("테스트 알림 1", "테스트 알림 2", "테스트 알림 3", "테스트 알림 4", "테스트 알림 5");
    }

    private Member createTestMember() {
        Member member = Member.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .authority("ROLE_USER")
                .creditScore(100)
                .build();
        return memberRepository.save(member);
    }

    private Product createTestProduct() {
        Member seller = Member.builder()
                .email("seller@example.com")
                .password("password123")
                .nickname("판매자")
                .authority("ROLE_USER")
                .creditScore(100)
                .build();
        memberRepository.save(seller);
        
        Product product = new Product(
                "테스트 상품",
                "테스트 상품 설명",
                DIGITAL_ELECTRONICS,
                10000L,
                java.time.LocalDateTime.now().plusHours(1),
                24,
                TRADE,
                "서울시",
                seller
        );
        return productRepository.save(product);
    }
}
