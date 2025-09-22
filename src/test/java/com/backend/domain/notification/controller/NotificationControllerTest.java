package com.backend.domain.notification.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

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
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
        setupTestData();
    }

    private void setupTestData() {
            // 테스트 회원 생성
            testMember = Member.builder()
                    .email("test@example.com")
                    .password("password123")
                    .nickname("testuser")
                    .authority("USER")
                    .creditScore(100)
                    .build();
            memberRepository.save(testMember);

            // 테스트 상품 생성
            testProduct = new Product(
                    "테스트 상품",
                    "테스트 상품 설명",
                    ProductCategory.DIGITAL_ELECTRONICS,
                    10000L,
                    LocalDateTime.now().plusMinutes(10),
                    24,
                    DeliveryMethod.DELIVERY,
                    "서울시 강남구",
                    testMember
            );
            productRepository.save(testProduct);

            // 읽은 알림 생성
            readNotification = new Notification(
                    "읽은 알림입니다.",
                    "NEW_BID",
                    true,
                    testMember,
                    testProduct
            );
            notificationRepository.save(readNotification);

            // 읽지 않은 알림 1
            unreadNotification1 = new Notification(
                    "읽지 않은 알림 1입니다.",
                    "OUTBID",
                    false,
                    testMember,
                    testProduct
            );
            notificationRepository.save(unreadNotification1);

            // 읽지 않은 알림 2
            unreadNotification2 = new Notification(
                    "읽지 않은 알림 2입니다.",
                    "AUCTION_WON",
                    false,
                    testMember,
                    testProduct
            );
            notificationRepository.save(unreadNotification2);
    }

    @Test
    @DisplayName("전체 알림 목록 조회 - 성공")
    void getNotifications_Success() throws Exception {
        mockMvc.perform(get("/notifications")
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
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("모든 알림이 읽음 처리되었습니다."))
                .andExpect(jsonPath("$.data").value(0)); // 처리된 알림이 0개
    }

    @Test
    @DisplayName("알림 응답 데이터 구조 검증")
    void getNotifications_ResponseStructure() throws Exception {
        mockMvc.perform(get("/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").exists())
                .andExpect(jsonPath("$.data.content[0].message").exists())
                .andExpect(jsonPath("$.data.content[0].type").exists())
                .andExpect(jsonPath("$.data.content[0].isRead").exists())
                .andExpect(jsonPath("$.data.content[0].productId").exists())
                .andExpect(jsonPath("$.data.content[0].productName").exists())
                .andExpect(jsonPath("$.data.content[0].createDate").exists());
    }

    @Test
    @DisplayName("잘못된 페이지 파라미터")
    void getNotifications_InvalidPageParameter() throws Exception {
        mockMvc.perform(get("/notifications")
                        .param("page", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(409)); // Spring Boot는 음수 페이지를 0으로 처리
    }
}
