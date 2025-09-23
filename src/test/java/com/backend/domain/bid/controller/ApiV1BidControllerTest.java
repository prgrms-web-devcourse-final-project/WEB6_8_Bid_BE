package com.backend.domain.bid.controller;

import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
@Transactional
@ActiveProfiles("test")
@WithMockUser
class ApiV1BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    private Member seller, bidder1, bidder2;
    private Product product1,product2;


    @BeforeEach
    void setUp() {
        // 모든 테스트 데이터 삭제
        bidRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트용 회원 생성
        bidder1 = memberRepository.save(Member.builder().email("user1@example.com").password("password").build());
        bidder2 = memberRepository.save(Member.builder().email("user2@example.com").password("password").build());
        seller = memberRepository.save(Member.builder().email("seller@example.com").password("password").build());

        // 경매 상품 생성 (판매자는 seller)
        product1 = productRepository.save(new Product(
                "Product A", "Description A", ProductCategory.DIGITAL_ELECTRONICS,
                1000L, LocalDateTime.now(), 24, DeliveryMethod.DELIVERY, "Seoul", seller
        ));
        product2 = productRepository.save(new Product(
                "Product B", "Description B", ProductCategory.DIGITAL_ELECTRONICS,
                2000L, LocalDateTime.now(), 24, DeliveryMethod.DELIVERY, "Seoul", seller
        ));
        product1.setCurrentPrice(1200000L);
//        product1.setStatus(AuctionStatus.BIDDING.getDisplayName());
        productRepository.save(product1);
        product1.setCurrentPrice(1200000L);
//        product1.setStatus(AuctionStatus.BIDDING.getDisplayName());
        productRepository.save(product2);

        // t8 테스트용 입찰 데이터 생성
        bidRepository.save(Bid.builder()
                .bidPrice(1500000L)
                .product(product1)
                .member(bidder1)
                .status("winning")
                .build());

        bidRepository.save(Bid.builder()
                .bidPrice(1400000L)
                .product(product1)
                .member(bidder2)
                .status("losing")
                .build());
    }


    @Test
    @DisplayName("입찰 등록 성공")
    @WithMockUser(username = "2")
    void t1() throws Exception {
        // Given
        BidRequestDto bidRequestDto = new BidRequestDto(1300000L);
        String requestBody = objectMapper.writeValueAsString(bidRequestDto);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"));
    }

    @Test
    @DisplayName("입찰 등록 실패 - 유효성 검증 (가격 없음)")
    void t2() throws Exception {
        // Given
        Long productId = product1.getId();
        BidRequestDto requestDto = new BidRequestDto(null);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", productId)
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
        Long productId = product1.getId();
        BidRequestDto requestDto = new BidRequestDto(-1000L);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", productId)
                        .with(user(String.valueOf(bidder1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"));
    }

    @Test
    @DisplayName("입찰 현황 조회 성공")
    void t4() throws Exception {
        // Given
        Long productId = product1.getId();

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/products/{productId}", productId))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("입찰 현황이 조회되었습니다."))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.productName").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists())
                .andExpect(jsonPath("$.data.bidCount").exists())
                .andExpect(jsonPath("$.data.recentBids").isArray());
    }

    @Test
    @DisplayName("입찰 현황 조회 실패 - 존재하지 않는 상품")
    void t5() throws Exception {
        // Given
        Long nonExistentProductId = 999L;

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
    @DisplayName("내 입찰 내역 조회 성공 - 기본 페이징")
    @WithMockUser(username = "1")
    void t6() throws Exception {
        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me"))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("내 빈 입찰내역 조회 성공."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.hasNext").exists());
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 - 커스텀 페이징")
    @WithMockUser(username = "1")
    void t7() throws Exception {
        // Given
        int page = 1;
        int size = 5;

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.currentPage").value(page))
                .andExpect(jsonPath("$.data.pageSize").value(size));
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 - isWinning 플래그 확인")
    @WithMockUser(username = "1")
    void t8() throws Exception {
        // Given
        // bidder1이 product1에 1500원으로 입찰하여 1등 입찰자가 됨
        bidRepository.save(new Bid(1000L, "bidding", product1, bidder2));
        bidRepository.save(new Bid(1500L, "bidding", product1, bidder1));

        // bidder1이 product2에 2500원으로 입찰했지만, bidder2가 3000원으로 더 높은 입찰을 하여 밀려남
        bidRepository.save(new Bid(2500L, "bidding", product2, bidder1));
        bidRepository.save(new Bid(3000L, "bidding", product2, bidder2));

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me"))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.productId == 1)].isWinning").value(true))
                .andExpect(jsonPath("$.data.content[?(@.productId == 2)].isWinning").value(false));
    }
}