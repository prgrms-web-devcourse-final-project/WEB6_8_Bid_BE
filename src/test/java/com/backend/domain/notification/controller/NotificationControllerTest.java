package com.backend.domain.notification.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.redis.TestRedisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@WithMockUser
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member testMember;
    private Product testProduct;
    private Notification readNotification;
    private Notification unreadNotification1;
    private Notification unreadNotification2;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // TestInitData에서 생성된 멤버 사용
        testMember = memberRepository.findByNickname("입찰자1").get();
        
        // TestInitData에서 생성된 첫 번째 상품 사용
        List<Product> products = productRepository.findAll();
        testProduct = products.stream()
                .filter(p -> p.getProductName().contains("iPhone 15 Pro"))
                .findFirst()
                .orElse(products.get(0)); // 첫 번째 상품

        // 기존 데이터 정리
        notificationRepository.deleteAll();

        // 알림 생성
        readNotification = new Notification();
        readNotification.setMessage(testProduct.getProductName() + " 상품에 새로운 입찰이 등록되었습니다.");
        readNotification.setNotificationType("BID_SUCCESS");
        readNotification.setIsRead(true);
        readNotification.setMember(testMember);
        readNotification.setProduct(testProduct);
        readNotification.setQueueStatus(Notification.QueueStatus.SENT);
        readNotification.setRetryCount(0);
        notificationRepository.save(readNotification);

        unreadNotification1 = new Notification();
        unreadNotification1.setMessage(testProduct.getProductName() + " 상품에서 새로운 입찰이 들어와 밀렸습니다.");
        unreadNotification1.setNotificationType("BID_OUTBID");
        unreadNotification1.setIsRead(false);
        unreadNotification1.setMember(testMember);
        unreadNotification1.setProduct(testProduct);
        unreadNotification1.setQueueStatus(Notification.QueueStatus.SENT);
        unreadNotification1.setRetryCount(0);
        notificationRepository.save(unreadNotification1);

        unreadNotification2 = new Notification();
        unreadNotification2.setMessage("축하합니다! " + testProduct.getProductName() + " 상품을 낙찰받았습니다!");
        unreadNotification2.setNotificationType("AUCTION_WON");
        unreadNotification2.setIsRead(false);
        unreadNotification2.setMember(testMember);
        unreadNotification2.setProduct(testProduct);
        unreadNotification2.setQueueStatus(Notification.QueueStatus.SENT);
        unreadNotification2.setRetryCount(0);
        notificationRepository.save(unreadNotification2);
    }

    @Test
    @DisplayName("전체 알림 목록 조회 - 성공")
    void getNotifications_Success() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("알림 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    @DisplayName("읽지 않은 알림만 조회 - 성공")
    void getNotifications_UnreadOnly_Success() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "0")
                        .param("size", "10")
                        .param("isRead", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("알림 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    @DisplayName("읽은 알림만 조회 - 성공")
    void getNotifications_ReadOnly_Success() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "0")
                        .param("size", "10")
                        .param("isRead", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    @DisplayName("페이징 처리 확인")
    void getNotifications_Paging_Success() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("특정 알림 읽음 처리 - 성공")
    void markAsRead_Success() throws Exception {
        Long notificationId = unreadNotification1.getId();

        mockMvc.perform(put("/notifications/{id}/read", notificationId)
                        .with(user(String.valueOf(testMember.getId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("알림이 읽음 처리되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        // 읽음 처리 확인
        Notification updatedNotification = notificationRepository.findById(notificationId).orElse(null);
        assert updatedNotification != null;
        assert updatedNotification.getIsRead() == true;
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 - 실패")
    void markAsRead_NotFound() throws Exception {
        Long nonExistentId = 999999L;

        mockMvc.perform(put("/notifications/{id}/read", nonExistentId)
                        .with(user(String.valueOf(testMember.getId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404"))
                .andExpect(jsonPath("$.msg").value("알림을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 - 성공")
    void markAllAsRead_Success() throws Exception {
        mockMvc.perform(put("/notifications/read-all")
                        .with(user(String.valueOf(testMember.getId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("모든 알림이 읽음 처리되었습니다."))
                .andExpect(jsonPath("$.data").value(2)); // 읽지 않은 알림 2개가 처리됨

        // 모든 알림이 읽음 처리되었는지 확인
        Integer unreadCount = notificationRepository.countUnreadNotifications(testMember.getId());
        assert unreadCount == 0;
    }

    @Test
    @DisplayName("이미 모든 알림을 읽은 상태에서 전체 읽음 처리")
    void markAllAsRead_AlreadyRead() throws Exception {
        // given - 모든 알림을 먼저 읽음 처리
        notificationRepository.markAllAsRead(testMember.getId());

        mockMvc.perform(put("/notifications/read-all")
                        .with(user(String.valueOf(testMember.getId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("모든 알림이 읽음 처리되었습니다."))
                .andExpect(jsonPath("$.data").value(0)); // 처리된 알림이 0개
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - 성공")
    void getUnreadCount_Success() throws Exception {
        mockMvc.perform(get("/notifications/unread-count")
                        .with(user(String.valueOf(testMember.getId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("읽지 않은 알림 개수가 조회되었습니다."))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("알림 응답 데이터 구조 검증")
    void getNotifications_ResponseStructure() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").exists())
                .andExpect(jsonPath("$.data.content[0].message").exists())
                .andExpect(jsonPath("$.data.content[0].type").exists())
                .andExpect(jsonPath("$.data.content[0].isRead").exists())
                .andExpect(jsonPath("$.data.content[0].productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.data.content[0].productName").value(testProduct.getProductName()))
                .andExpect(jsonPath("$.data.content[0].createDate").exists());
    }

    @Test
    @DisplayName("알림 내용과 타입 검증")
    void getNotifications_ContentValidation() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // 최신순으로 정렬되므로 unreadNotification2가 첫 번째
                .andExpect(jsonPath("$.data.content[0].type").value("AUCTION_WON"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false))
                // 두 번째는 unreadNotification1
                .andExpect(jsonPath("$.data.content[1].type").value("BID_OUTBID"))
                .andExpect(jsonPath("$.data.content[1].isRead").value(false))
                // 세 번째는 readNotification
                .andExpect(jsonPath("$.data.content[2].type").value("BID_SUCCESS"))
                .andExpect(jsonPath("$.data.content[2].isRead").value(true));
    }

    @Test
    @DisplayName("잘못된 페이지 파라미터")
    void getNotifications_InvalidPageParameter() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("다른 사용자의 알림은 조회되지 않음")
    void getNotifications_OnlyOwnNotifications() throws Exception {
        // 다른 사용자의 알림 생성
        Member otherMember = memberRepository.findByNickname("입찰자2").get();
        List<Product> products = productRepository.findAll();
        Product anotherProduct = products.size() > 1 ? products.get(1) : products.get(0);
        
        Notification otherUserNotification = new Notification();
        otherUserNotification.setMessage("다른 사용자의 알림입니다.");
        otherUserNotification.setNotificationType("BID_SUCCESS");
        otherUserNotification.setIsRead(false);
        otherUserNotification.setMember(otherMember);
        otherUserNotification.setProduct(anotherProduct);
        otherUserNotification.setQueueStatus(Notification.QueueStatus.SENT);
        otherUserNotification.setRetryCount(0);
        notificationRepository.save(otherUserNotification);

        // 현재 테스트 멤버의 알림만 조회됨
        mockMvc.perform(get("/notifications")
                        .with(user(String.valueOf(testMember.getId())))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3)) // 여전히 3개만
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }
}
