package com.backend.domain.bid.controller;

import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

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
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    // TestInitData에서 생성된 데이터 활용
    private Member getBidder1() {
        return memberRepository.findByNickname("입찰자1").get();
    }

    private Member getBidder2() {
        return memberRepository.findByNickname("입찰자2").get();
    }

    private Member getSeller() {
        return memberRepository.findByNickname("판매자").get();
    }

    private Product getTestProduct() {
        // TestInitData에서 생성된 상품들 중 입찰이 있는 상품
        List<Product> products = productRepository.findAll();
        return products.stream()
                .filter(p -> p.getProductName().contains("아이폰 15 Pro 256GB"))
                .findFirst()
                .orElse(products.get(3));
    }

    private Product getActiveProduct() {
        // TestInitData에서 생성된 첫 번째 상품 사용
        List<Product> products = productRepository.findAll();
        return products.stream()
                .filter(p -> p.getProductName().contains("iPhone 15 Pro"))
                .findFirst()
                .orElse(products.get(0));
    }

    @Test
    @DisplayName("입찰 등록 성공")
    void t1() throws Exception {
        // Given - TestInitData의 입찰자2와 활성 상품 사용
        Member bidder = getBidder2();
        Product product = getActiveProduct();
        BidRequestDto bidRequestDto = new BidRequestDto(1100000L); // 기존 시작가보다 높게

        String requestBody = objectMapper.writeValueAsString(bidRequestDto);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", product.getId())
                        .with(user(String.valueOf(bidder.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value("입찰이 완료되었습니다."))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.price").value(1100000L));
    }

    @Test
    @DisplayName("입찰 등록 실패 - 유효성 검증 (가격 없음)")
    void t2() throws Exception {
        // Given
        Member bidder = getBidder1();
        Product product = getActiveProduct();
        BidRequestDto requestDto = new BidRequestDto(null);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", product.getId())
                        .with(user(String.valueOf(bidder.getId())))
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
        Member bidder = getBidder1();
        Product product = getActiveProduct();
        BidRequestDto requestDto = new BidRequestDto(-1000L);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", product.getId())
                        .with(user(String.valueOf(bidder.getId())))
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
    @DisplayName("입찰 등록 실패 - 현재 최고가보다 낮은 입찰")
    void t4() throws Exception {
        // Given - TestInitData에서 이미 입찰이 있는 상품 사용
        Product product = getTestProduct(); // 입찰이 있는 상품
        Member bidder = getBidder1();
        BidRequestDto requestDto = new BidRequestDto(1000000L); // 현재 최고가보다 낮게 설정

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", product.getId())
                        .with(user(String.valueOf(bidder.getId())))
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
    void t5() throws Exception {
        // Given
        Member seller = getSeller();
        Product product = getActiveProduct(); // 판매자가 등록한 상품
        BidRequestDto requestDto = new BidRequestDto(1500000L);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/bids/products/{productId}", product.getId())
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
    void t6() throws Exception {
        // Given - 임의의 상품 사용
        Product product = getActiveProduct();

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/products/{productId}", product.getId()))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("입찰 현황이 조회되었습니다."))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.productName").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists())
                .andExpect(jsonPath("$.data.initialPrice").exists())
                .andExpect(jsonPath("$.data.bidCount").exists())
                .andExpect(jsonPath("$.data.status").exists())
                .andExpect(jsonPath("$.data.recentBids").isArray());
    }

    @Test
    @DisplayName("입찰 현황 조회 실패 - 존재하지 않는 상품")
    void t7() throws Exception {
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
    @DisplayName("내 입찰 내역 조회 성공 - 입찰 내역 있는 사용자")
    void t8() throws Exception {
        // Given - TestInitData에서 입찰한 사용자 (입찰자1)
        Member bidder = getBidder1();

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me")
                        .with(user(String.valueOf(bidder.getId()))))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 - 입찰 내역 없는 사용자")
    void t9() throws Exception {
        // Given - TestInitData에서 입찰하지 않은 사용자 (판매자)
        Member seller = getSeller();

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
    void t10() throws Exception {
        // Given
        Member bidder = getBidder1();
        int page = 0;
        int size = 5;

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(user(String.valueOf(bidder.getId()))))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.currentPage").value(page))
                .andExpect(jsonPath("$.data.pageSize").value(size));
    }

    @Test
    @DisplayName("내 입찰 내역 조회 - 페이징 결과 검증")
    void t11() throws Exception {
        // Given - TestInitData에서 입찰자2 (입찰 내역이 있을 수 있음)
        Member bidder = getBidder2();

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/bids/me")
                        .with(user(String.valueOf(bidder.getId()))))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.hasNext").exists());
    }
}
