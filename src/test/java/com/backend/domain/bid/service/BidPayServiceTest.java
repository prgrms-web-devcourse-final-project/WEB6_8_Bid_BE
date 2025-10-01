package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.BidPayResponseDto;
import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class BidPayServiceTest {

    @Autowired BidService bidService;
    @Autowired BidRepository bidRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired CashRepository cashRepository;
    @Autowired EntityManager em;

    Member me;         // 입찰자(구매자)
    Member seller;     // 판매자(상품의 seller)
    Product product;   // 낙찰 상태의 상품

    @BeforeEach
    void setUp() {
        // 1) 회원(구매자/판매자) 생성
        seller = memberRepository.save(
                Member.builder()
                        .email("seller@test.com")
                        .password("pw!")
                        .nickname("seller")
                        .build()
        );
        me = memberRepository.save(
                Member.builder()
                        .email("winner@test.com")
                        .password("pw!")
                        .nickname("winner")
                        .build()
        );

        // 2) Product는 생성자 필수값이 많으니 "생성자"로 만든다.
        LocalDateTime start = LocalDateTime.now().minusHours(2); // 이미 시작
        Integer durationHrs = 1; // 1시간 경매 → 이미 종료로 간주
        product = new Product(
                "테스트 상품",
                "설명",
                ProductCategory.values()[0],  // 프로젝트 실제 카테고리로 교체 가능
                1_000L,
                start,
                durationHrs,
                DeliveryMethod.values()[0],   // 프로젝트 실제 배송방법으로 교체 가능
                "서울",
                seller
        );
        // 상태/현재가만 setter로 덮어쓰기
        product.setStatus(AuctionStatus.SUCCESSFUL.getDisplayName()); // "낙찰"
        product.setCurrentPrice(7_000L);
        em.persist(product);

        // 3) 지갑: 잔액 10_000 (me = 구매자)
        cashRepository.save(Cash.builder()
                .member(me)
                .balance(10_000L)
                .build());
    }

    @Test
    void 낙찰_결제_정상동작() {
        // 내 입찰(최고가 7_000)
        Bid myBid = Bid.builder()
                .bidPrice(7_000L)
                .status("bidding")
                .product(product)
                .member(me)
                .build();
        bidRepository.save(myBid);

        // 실행
        RsData<BidPayResponseDto> rs = bidService.payForBid(me.getId(), myBid.getId());

        // 검증: record 접근자(resultCode()/msg()/data())
        assertThat(rs.resultCode()).isEqualTo("200");
        assertThat(rs.data().amount()).isEqualTo(7_000L);
        assertThat(rs.data().paidAt()).isNotNull();
        assertThat(rs.data().cashTransactionId()).isNotNull();
        assertThat(rs.data().balanceAfter()).isEqualTo(3_000L); // 10_000 - 7_000
    }

    @Test
    void 이미_결제한_입찰은_멱등_응답() {
        // 이미 결제된 입찰 만들기
        Bid bid = Bid.builder()
                .bidPrice(7_000L)
                .status("bidding")
                .product(product)
                .member(me)
                .paidAt(LocalDateTime.now())
                .paidAmount(7_000L)
                .build();
        bidRepository.save(bid);

        RsData<BidPayResponseDto> rs = bidService.payForBid(me.getId(), bid.getId());

        assertThat(rs.resultCode()).isEqualTo("200");
        assertThat(rs.msg()).contains("이미 결제");
        assertThat(rs.data().amount()).isEqualTo(7_000L);
        // 간단 버전: cashTransactionId / balanceAfter 는 null일 수 있음
    }

    @Test
    void 낙찰상태가_아니면_결제_거부() {
        // "경매 중" 상품 하나 더 만들어서 실패 유도
        LocalDateTime start = LocalDateTime.now().minusMinutes(10);
        Integer durationHrs = 2; // 아직 진행 중
        Product biddingProduct = new Product(
                "테스트 상품2",
                "설명2",
                ProductCategory.values()[0],
                2_000L,
                start,
                durationHrs,
                DeliveryMethod.values()[0],
                "부산",
                seller
        );
        biddingProduct.setStatus(AuctionStatus.BIDDING.getDisplayName()); // "경매 중"
        biddingProduct.setCurrentPrice(7_000L);
        em.persist(biddingProduct);

        Bid myBid = Bid.builder()
                .bidPrice(7_000L)
                .status("bidding")
                .product(biddingProduct)
                .member(me)
                .build();
        bidRepository.save(myBid);

        assertThatThrownBy(() -> bidService.payForBid(me.getId(), myBid.getId()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("낙찰이 확정되지");
    }
}
