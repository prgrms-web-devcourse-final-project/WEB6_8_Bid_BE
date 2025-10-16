package com.backend.domain.notification.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.StandardProduct;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.redis.TestRedisConfiguration;
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

import java.util.List;

import static com.backend.domain.product.enums.DeliveryMethod.TRADE;
import static com.backend.domain.product.enums.ProductCategory.DIGITAL_ELECTRONICS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@MockitoSettings
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
class BidNotificationServiceTest {

    @Autowired
    private BidNotificationService bidNotificationService;
    
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
    @DisplayName("입찰 성공 알림 테스트")
    void notifyBidSuccess() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        Long bidAmount = 50000L;

        // When
        bidNotificationService.notifyBidSuccess(member.getId(), product, bidAmount);

        // Then
        verify(webSocketService, times(1)).sendNotificationToUser(
                eq(member.getEmail()), 
                contains("입찰했습니다"), 
                any()
        );
        
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        Notification savedNotification = notifications.get(0);
        assertThat(savedNotification.getMember().getEmail()).isEqualTo(member.getEmail());
        assertThat(savedNotification.getProduct().getId()).isEqualTo(product.getId());
        assertThat(savedNotification.getNotificationType()).isEqualTo("BID_SUCCESS");
        assertThat(savedNotification.getMessage()).contains(product.getProductName());
        assertThat(savedNotification.getMessage()).contains("50,000");
        assertThat(savedNotification.getQueueStatus()).isEqualTo(Notification.QueueStatus.PENDING);
    }

    @Test
    @DisplayName("입찰 밀림 알림 테스트")
    void notifyBidOutbid() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        Long myBidAmount = 30000L;
        Long newHighestBid = 40000L;

        // When
        bidNotificationService.notifyBidOutbid(member.getId(), product, myBidAmount, newHighestBid);

        // Then
        verify(webSocketService, times(1)).sendNotificationToUser(
                eq(member.getEmail()), 
                contains("밀렸습니다"), 
                any()
        );
        
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        Notification savedNotification = notifications.get(0);
        assertThat(savedNotification.getNotificationType()).isEqualTo("BID_OUTBID");
        assertThat(savedNotification.getMessage()).contains("40,000"); // 천단위 구분자 포함
    }

    @Test
    @DisplayName("낙찰 알림 테스트")
    void notifyAuctionWon() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        Long finalPrice = 100000L;

        // When
        bidNotificationService.notifyAuctionWon(member.getId(), product, finalPrice);

        // Then
        verify(webSocketService, times(1)).sendNotificationToUser(
                eq(member.getEmail()), 
                contains("낙찰받았습니다"), 
                any()
        );
        
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        Notification savedNotification = notifications.get(0);
        assertThat(savedNotification.getNotificationType()).isEqualTo("AUCTION_WON");
        assertThat(savedNotification.getMessage()).contains("축하합니다");
    }

    @Test
    @DisplayName("낙찰 실패 알림 테스트")
    void notifyAuctionLost() {
        // Given
        Member member = createTestMember();
        Product product = createTestProduct();
        Long finalPrice = 80000L;
        Long myBidAmount = 70000L;

        // When
        bidNotificationService.notifyAuctionLost(member.getId(), product, finalPrice, myBidAmount);

        // Then
        verify(webSocketService, times(1)).sendNotificationToUser(
                eq(member.getEmail()), 
                contains("경매가 종료되었습니다"), 
                any()
        );
        
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        Notification savedNotification = notifications.get(0);
        assertThat(savedNotification.getNotificationType()).isEqualTo("AUCTION_LOST");
    }

    @Test
    @DisplayName("존재하지 않는 사용자ID로 알림 발송 시 DB 저장 안됨")
    void notifyWithInvalidUserId() {
        // Given
        Product product = createTestProduct();
        Long invalidUserId = 999999L;
        Long bidAmount = 50000L;

        // When
        bidNotificationService.notifyBidSuccess(invalidUserId, product, bidAmount);

        // Then
        // member가 null이므로 sendNotificationToUser가 호출되지 않아야 함
        verify(webSocketService, times(0)).sendNotificationToUser(any(), any(), any());
        
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).isEmpty();
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
        
        Product product = new StandardProduct(
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
