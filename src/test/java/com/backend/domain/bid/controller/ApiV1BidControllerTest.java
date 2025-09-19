package com.backend.domain.bid.controller;

import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.repository.BidRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ApiV1BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BidRepository bidRepository;

//    TODO: 나중에 추가
//    @Autowired
//    private MemberRepository memberRepository;
//
//    @Autowired
//    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전에 데이터 정리
        bidRepository.deleteAll();
        // TODO: 테스트용 Member, Product 데이터 생성
    }

    @Test
    @DisplayName("입찰 등록 성공")
    void t1() throws Exception {
        // Given
        Long productId = 1L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value("입찰이 완료되었습니다."))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.bidderId").value(1))
                .andExpect(jsonPath("$.data.price").value(1100000))
                .andExpect(jsonPath("$.data.status").value("bidding"));
    }

    @Test
    @DisplayName("입찰 등록 실패 - 유효성 검증 (가격 없음)")
    void t2() throws Exception {
        // Given
        Long productId = 1L;
        BidRequestDto requestDto = new BidRequestDto(null);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", productId)
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
        Long productId = 1L;
        BidRequestDto requestDto = new BidRequestDto(-1000L);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", productId)
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
        Long productId = 1L;

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/products/{productId}", productId))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("입찰 현황 조회 성공"))
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
    void t6() throws Exception {
        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me"))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("내 입찰 내역 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.hasNext").exists());
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 - 커스텀 페이징")
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
    void t8() throws Exception {
        // Given: 테스트용 입찰 데이터 생성 필요
        // TODO: 1등인 입찰과 밀린 입찰을 생성

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me"))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].isWinning").exists())
                .andExpect(jsonPath("$.data.content[*].myBidPrice").exists())
                .andExpect(jsonPath("$.data.content[*].currentPrice").exists());
    }
}
