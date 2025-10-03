package com.backend.domain.bid.controller;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.enums.BidStatus;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // JWT 등 보안 필터 비활성화(단순화)
@Transactional
class BidPayControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    CashRepository cashRepository;
    @Autowired
    BidRepository bidRepository;

    Member seller;
    Member buyer;
    Product product; // 낙찰 상태
    Bid bid;         // buyer의 최고 입찰(= currentPrice)

    @BeforeEach
    void setUp() {
        // 1) 판매자/구매자 (password는 NOT NULL)
        seller = memberRepository.save(
                Member.builder().email("seller@test.com").password("pw").nickname("seller").build()
        );
        buyer = memberRepository.save(
                Member.builder().email("buyer@test.com").password("pw").nickname("buyer").build()
        );

        // 2) 상품: 낙찰 상태 + 최고가 7,000
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        int duration = 1;
        product = new Product(
                "테스트 상품",
                "설명",
                ProductCategory.values()[0],
                1_000L,
                start,
                duration,
                DeliveryMethod.values()[0],
                "서울",
                seller
        );
        product.setStatus(AuctionStatus.SUCCESSFUL.getDisplayName()); // "낙찰"
        product.setCurrentPrice(7_000L);
        em.persist(product);

        // 3) 지갑: 10,000
        cashRepository.save(Cash.builder().member(buyer).balance(10_000L).build());

        // 4) 내 최고 입찰(7,000)
        bid = bidRepository.save(
                Bid.builder().product(product).member(buyer).bidPrice(7_000L).status(BidStatus.BIDDING).build()
        );
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
        // 컨트롤러가 이메일/숫자 둘 다 지원
    void 낙찰_결제_API_성공_200_RsData형식() throws Exception {
        mvc.perform(post("/api/v1/bids/{bidId}/pay", bid.getId()))
                .andDo(print())
                // GlobalExceptionHandler를 안 타는 정상 플로우 → 200대 resultCode
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode", startsWith("200")))
                .andExpect(jsonPath("$.msg", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(7_000)))
                .andExpect(jsonPath("$.data.balanceAfter", is(3_000)))
                .andExpect(jsonPath("$.data.paidAt", notNullValue()))
                .andExpect(jsonPath("$.data.cashTransactionId", notNullValue()));
    }

    @Test
    void 인증_없으면_401_바디검증없이상태만() throws Exception {
        // 컨트롤러에서 ResponseStatusException(401)을 던지며,
        // 이 경우 기본 핸들링으로 바디가 비어있을 수 있으니 상태코드만 확인
        mvc.perform(post("/api/v1/bids/{bidId}/pay", bid.getId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void 낙찰_상태가_아니면_400_RsData형식으로_msg존재() throws Exception {
        // 실패 유도: 경매중으로 바꿈
        product.setStatus(AuctionStatus.BIDDING.getDisplayName()); // "경매 중"
        em.flush();

        mvc.perform(post("/api/v1/bids/{bidId}/pay", bid.getId()))
                .andDo(print())
                // ServiceException("400", "...") → GlobalExceptionHandler 가
                // status=400, body=RsData(resultCode="400-..." 또는 "400", msg="...") 로 변환
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode", startsWith("400")))
                .andExpect(jsonPath("$.msg", containsString("낙찰이 확정되지"))); // 서비스 예외 메시지 일부
    }
}
