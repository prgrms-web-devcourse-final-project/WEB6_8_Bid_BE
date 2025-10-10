package com.backend.domain.bid.controller;

import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("bidtest")
@WithMockUser
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
class ApiV1BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    private Member bidder1;
    private Member bidder2;
    private Member seller;
    private Product activeProduct;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        // 기존 데이터 정리
        productRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트 데이터 생성
        createTestMembers();
        createTestProducts();

        // 다시 조회
        bidder1 = memberRepository.findByNickname("테스트입찰자1").orElseThrow();
        bidder2 = memberRepository.findByNickname("테스트입찰자2").orElseThrow();
        seller = memberRepository.findByNickname("테스트판매자").orElseThrow();
        activeProduct = productRepository.findAll().stream()
                .filter(p -> p.getProductName().equals("테스트상품1"))
                .findFirst()
                .orElseThrow();
    }

    private void createTestMembers() {
        Member member1 = Member.builder()
                .email("test-bidder1@test.com")
                .password("password")
                .nickname("테스트입찰자1")
                .phoneNumber("010-1111-1111")
                .address("서울시")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(member1);

        Member member2 = Member.builder()
                .email("test-bidder2@test.com")
                .password("password")
                .nickname("테스트입찰자2")
                .phoneNumber("010-2222-2222")
                .address("서울시")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(member2);

        Member sellerMember = Member.builder()
                .email("test-seller@test.com")
                .password("password")
                .nickname("테스트판매자")
                .phoneNumber("010-3333-3333")
                .address("서울시")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(sellerMember);
    }

    private void createTestProducts() {
        Member seller = memberRepository.findByNickname("테스트판매자").orElseThrow();

        Product product = Product.testBuilder()
                .productName("테스트상품1")
                .description("테스트용 상품입니다")
                .category(ProductCategory.DIGITAL_ELECTRONICS)
                .initialPrice(1000000L)
                .currentPrice(1000000L)
                .status(AuctionStatus.BIDDING.getDisplayName())
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .duration(25)
                .deliveryMethod(DeliveryMethod.BOTH)
                .location("서울시")
                .seller(seller)
                .testBuild();
        productRepository.save(product);
    }

    @Test
    @DisplayName("입찰 등록 성공")
    void t1() throws Exception {
        // Given
        BidRequestDto bidRequestDto = new BidRequestDto(1100000L);
        String requestBody = objectMapper.writeValueAsString(bidRequestDto);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", activeProduct.getId())
                        .with(user(String.valueOf(bidder1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value("입찰이 완료되었습니다."))
                .andExpect(jsonPath("$.data.productId").value(activeProduct.getId()))
                .andExpect(jsonPath("$.data.price").value(1100000L));
    }

    @Test
    @DisplayName("입찰 등록 실패 - 유효성 검증 (가격 없음)")
    void t2() throws Exception {
        // Given
        BidRequestDto requestDto = new BidRequestDto(null);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", activeProduct.getId())
                        .with(user(String.valueOf(bidder1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("입찰 등록 실패 - 유효성 검증 (음수 가격)")
    void t3() throws Exception {
        // Given
        BidRequestDto requestDto = new BidRequestDto(-1000L);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", activeProduct.getId())
                        .with(user(String.valueOf(bidder1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("입찰 등록 실패 - 본인 상품에 입찰")
    void t4() throws Exception {
        // Given
        BidRequestDto requestDto = new BidRequestDto(1500000L);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", activeProduct.getId())
                        .with(user(String.valueOf(seller.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.msg").value("본인이 등록한 상품에는 입찰할 수 없습니다."));
    }

    @Test
    @DisplayName("입찰 현황 조회 성공")
    void t5() throws Exception {
        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/products/{productId}", activeProduct.getId()))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("입찰 현황이 조회되었습니다."))
                .andExpect(jsonPath("$.data.productId").value(activeProduct.getId()))
                .andExpect(jsonPath("$.data.productName").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists())
                .andExpect(jsonPath("$.data.initialPrice").exists())
                .andExpect(jsonPath("$.data.bidCount").exists())
                .andExpect(jsonPath("$.data.status").exists())
                .andExpect(jsonPath("$.data.recentBids").isArray());
    }

    @Test
    @DisplayName("입찰 현황 조회 실패 - 존재하지 않는 상품")
    void t6() throws Exception {
        // Given
        Long nonExistentProductId = 999999L;

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/products/{productId}", nonExistentProductId))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 상품입니다."));
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 - 입찰 내역 없는 사용자")
    void t7() throws Exception {
        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me")
                        .with(user(String.valueOf(seller.getId()))))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 - 커스텀 페이징")
    void t8() throws Exception {
        // Given
        int page = 0;
        int size = 5;

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(user(String.valueOf(bidder1.getId()))))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.currentPage").value(page))
                .andExpect(jsonPath("$.data.pageSize").value(size));
    }
}
