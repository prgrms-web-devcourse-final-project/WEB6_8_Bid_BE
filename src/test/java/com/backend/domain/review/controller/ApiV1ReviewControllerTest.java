package com.backend.domain.review.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.StandardProduct;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.domain.review.dto.ReviewRequest;
import com.backend.domain.review.entity.Review;
import com.backend.domain.review.repository.ReviewRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.redis.TestRedisConfiguration;
import com.backend.global.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
@Transactional
class ApiV1ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Member testUser;
    private Member anotherUser;
    private Product testProduct;
    private String testUserToken;
    private String anotherUserToken;

    @BeforeEach
    void setUp() {
        testUser = memberRepository.save(Member.builder().email("test@example.com").password("password").nickname("testuser").build());
        anotherUser = memberRepository.save(Member.builder().email("another@user.com").password("pw").nickname("another").build());
        Member seller = memberRepository.save(Member.builder().email("seller@example.com").password("password").nickname("seller").build());
        testProduct = productRepository.save(StandardProduct.testBuilder()
                .seller(seller)
                .productName("Test Product")
                .category(ProductCategory.DIGITAL_ELECTRONICS)
                .initialPrice(10000L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .duration(24)
                .status(AuctionStatus.BIDDING.getDisplayName())
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .testBuild());

        testUserToken = jwtUtil.generateAccessToken(testUser.getEmail());
        anotherUserToken = jwtUtil.generateAccessToken(anotherUser.getEmail());
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReview_Success() throws Exception {
        // Given
        ReviewRequest request = new ReviewRequest(testProduct.getId(), "Great product!", true);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + testUserToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value("리뷰가 성공적으로 등록되었습니다."))
                .andExpect(jsonPath("$.data.comment").value("Great product!"))
                .andExpect(jsonPath("$.data.isSatisfied").value(true));

        // DB 검증
        Review review = reviewRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(review.getComment()).isEqualTo("Great product!");
        assertThat(review.getReviewer().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("리뷰 조회 성공")
    void getReview_Success() throws Exception {
        // Given
        Review review = reviewRepository.save(Review.builder()
                .reviewer(testUser)
                .product(testProduct)
                .comment("Existing review")
                .isSatisfied(false)
                .build());

        // Lazy-loading을 위한 프록시 강제 초기화
        review.getReviewer().getNickname();

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/{reviewId}", review.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.reviewId").value(review.getId()))
                .andExpect(jsonPath("$.data.comment").value("Existing review"))
                .andExpect(jsonPath("$.data.reviewerNickname").value(testUser.getNickname()));
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_Success() throws Exception {
        // Given
        Review review = reviewRepository.save(Review.builder()
                .reviewer(testUser)
                .product(testProduct)
                .comment("Old comment")
                .isSatisfied(true)
                .build());

        ReviewRequest request = new ReviewRequest(testProduct.getId(), "Updated comment", false);

        // When & Then
        mockMvc.perform(put("/api/v1/reviews/{reviewId}", review.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.comment").value("Updated comment"))
                .andExpect(jsonPath("$.data.isSatisfied").value(false));

        // DB 검증
        Review updatedReview = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(updatedReview.getComment()).isEqualTo("Updated comment");
        assertThat(updatedReview.getIsSatisfied()).isFalse();
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 권한 없음")
    void updateReview_AccessDenied() throws Exception {
        // Given
        Review review = reviewRepository.save(Review.builder()
                .reviewer(testUser) // 리뷰는 testUser가 작성
                .product(testProduct)
                .comment("Old comment")
                .isSatisfied(true)
                .build());

        ReviewRequest request = new ReviewRequest(testProduct.getId(), "Updated comment", false);

        // When & Then
        mockMvc.perform(put("/api/v1/reviews/{reviewId}", review.getId())
                        .header("Authorization", "Bearer " + anotherUserToken) // 다른 사용자 토큰으로 요청
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403"));
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_Success() throws Exception {
        // Given
        Review review = reviewRepository.save(Review.builder()
                .reviewer(testUser)
                .product(testProduct)
                .comment("To be deleted")
                .isSatisfied(true)
                .build());

        // When & Then
        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", review.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("리뷰가 성공적으로 삭제되었습니다."));

        // DB 검증
        assertThat(reviewRepository.findById(review.getId())).isEmpty();
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 권한 없음")
    void deleteReview_AccessDenied() throws Exception {
        // Given
        Review review = reviewRepository.save(Review.builder()
                .reviewer(testUser) // 리뷰는 testUser가 작성
                .product(testProduct)
                .comment("To be deleted")
                .isSatisfied(true)
                .build());

        // When & Then
        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", review.getId())
                        .header("Authorization", "Bearer " + anotherUserToken) // 다른 사용자 토큰으로 요청
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403"));
    }
}
