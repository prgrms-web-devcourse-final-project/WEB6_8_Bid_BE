package com.backend.domain.notification.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.websocket.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.backend.domain.product.enums.DeliveryMethod.TRADE;
import static com.backend.domain.product.enums.ProductCategory.DIGITAL_ELECTRONICS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@MockitoSettings
@Import(TestElasticsearchConfiguration.class)
class NotificationQueueIntegrationTest {

    @Autowired
    private NotificationQueueService notificationQueueService;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @MockitoBean
    private WebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("큐 처리 전체 플로우 테스트")
    void queueProcessingFlow() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        
        // 여러 알림 생성
        notificationQueueService.enqueueNotification(member, "알림 1", "BID_SUCCESS", product);
        notificationQueueService.enqueueNotification(member, "알림 2", "BID_OUTBID", product);
        notificationQueueService.enqueueNotification(member, "알림 3", "AUCTION_END", product);

        // 초기 상태 확인
        NotificationQueueService.QueueStatus initialStatus = notificationQueueService.getQueueStatus();
        assertThat(initialStatus.getPendingCount()).isEqualTo(3);

        // When - 큐 처리 실행
        notificationQueueService.processNotificationQueue();

        // 잠시 대기 (비동기 처리)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        NotificationQueueService.QueueStatus finalStatus = notificationQueueService.getQueueStatus();
        assertThat(finalStatus.getSentCount()).isGreaterThanOrEqualTo(3);
        
        // 알림 상태 직접 확인
        List<Notification> notifications = notificationRepository.findAll();
        long processedCount = notifications.stream()
                .filter(n -> n.getQueueStatus() == Notification.QueueStatus.SENT)
                .count();
        
        assertThat(processedCount).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("예약 알림 처리 테스트")
    void scheduledNotificationProcessing() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        
        // 과거 시간 예약 알림 (처리되어야 함)
        Notification pastNotification = notificationQueueService.enqueueScheduledNotification(
                member, "과거 예약 알림", "AUCTION_START", product, 
                LocalDateTime.now().minusMinutes(5));
        
        // 미래 시간 예약 알림 (처리되지 않아야 함)
        Notification futureNotification = notificationQueueService.enqueueScheduledNotification(
                member, "미래 예약 알림", "AUCTION_START", product, 
                LocalDateTime.now().plusMinutes(10));

        // When
        notificationQueueService.processNotificationQueue();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        Notification updatedPast = notificationRepository.findById(pastNotification.getId()).orElse(null);
        Notification updatedFuture = notificationRepository.findById(futureNotification.getId()).orElse(null);
        
        assertThat(updatedPast).isNotNull();
        assertThat(updatedFuture).isNotNull();
        
        // 과거 예약 알림은 처리됨
        assertThat(updatedPast.getQueueStatus()).isEqualTo(Notification.QueueStatus.SENT);
        
        // 미래 예약 알림은 처리되지 않음
        assertThat(updatedFuture.getQueueStatus()).isEqualTo(Notification.QueueStatus.PENDING);
    }

    @Test
    @DisplayName("큐 상태 조회 테스트")
    void getQueueStatus() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        
        // 다양한 상태의 알림 생성
        notificationQueueService.enqueueNotification(member, "대기중", "BID_SUCCESS", product);
        
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
        assertThat(status.getPendingCount()).isGreaterThanOrEqualTo(1);
        assertThat(status.getProcessingCount()).isGreaterThanOrEqualTo(1);
        assertThat(status.getSentCount()).isGreaterThanOrEqualTo(1);
        assertThat(status.getFailedCount()).isGreaterThanOrEqualTo(1);
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
