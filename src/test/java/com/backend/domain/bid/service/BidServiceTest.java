package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.BidCurrentResponseDto;
import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.dto.BidResponseDto;
import com.backend.domain.bid.dto.MyBidResponseDto;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BidServiceTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

//    @Autowired
//    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // TODO:테스트 데이터 생성
    }

    @Test
    @DisplayName("입찰 등록 성공 테스트")
    void t1() {
        // Given: 입찰 요청 데이터가 주어졌을 때
        Long productId = 1L;
        Long bidderId = 1L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When: 입찰을 시도하면
        RsData<BidResponseDto> rsData = bidService.createBid(productId, bidderId, requestDto);

        // Then: 성공 응답을 받고, 데이터베이스에 입찰이 저장되어야 한다.
        assertThat(rsData.resultCode()).isEqualTo("201");
        assertThat(rsData.msg()).isEqualTo("입찰이 완료되었습니다.");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().productId()).isEqualTo(productId);
        assertThat(rsData.data().bidderId()).isEqualTo(bidderId);
        assertThat(rsData.data().price()).isEqualTo(1100000L);
        assertThat(rsData.data().status()).isEqualTo("bidding");

        // DB에 실제로 저장되었는지 확인
        assertThat(bidRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 존재하지 않는 상품")
    void t2() {
        // Given: 존재하지 않는 상품 ID가 주어졌을 때
        Long nonExistentProductId = 999L;
        Long bidderId = 1L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생해야 한다.
        assertThatThrownBy(() -> bidService.createBid(nonExistentProductId, bidderId, requestDto))
                .isInstanceOf(ServiceException.class)
                .hasMessage("404:존재하지 않는 상품입니다.");
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 존재하지 않는 사용자")
    void t3() {
        // Given: 존재하지 않는 사용자 ID가 주어졌을 때
        Long productId = 1L;
        Long nonExistentBidderId = 999L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생해야 한다.
        assertThatThrownBy(() -> bidService.createBid(productId, nonExistentBidderId, requestDto))
                .isInstanceOf(ServiceException.class)
                .hasMessage("404:존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 현재 최고가보다 낮은 금액")
    void t4() {
        // Given: 현재 최고가보다 낮은 입찰 금액이 주어졌을 때
        Long productId = 1L;
        Long bidderId = 1L;
        
        // 먼저 높은 금액으로 입찰
        BidRequestDto highBidRequest = new BidRequestDto(1200000L);
        bidService.createBid(productId, bidderId, highBidRequest);
        
        // 그 다음 낮은 금액으로 입찰 시도
        BidRequestDto lowBidRequest = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생해야 한다.
        assertThatThrownBy(() -> bidService.createBid(productId, bidderId, lowBidRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("현재 최고가");
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 1000원 단위가 아닌 금액")
    void t5() {
        // Given: 1000원 단위가 아닌 입찰 금액이 주어졌을 때
        Long productId = 1L;
        Long bidderId = 1L;
        BidRequestDto requestDto = new BidRequestDto(1100500L); // 500원 단위

        // When & Then: 입찰을 시도하면 ServiceException이 발생해야 한다.
        assertThatThrownBy(() -> bidService.createBid(productId, bidderId, requestDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("1000원 단위");
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 최소 증가 금액 미만")
    void t6() {
        // Given: 현재가보다 100원 미만으로 높은 입찰 금액이 주어졌을 때
        Long productId = 1L;
        Long bidderId = 1L;
        
        // 먼저 입찰
        BidRequestDto firstBid = new BidRequestDto(1100000L);
        bidService.createBid(productId, bidderId, firstBid);
        
        // 1000원 정확히만 높게 입찰 시도 (최소 증가 금액인 1000원보다 작음)
        BidRequestDto exactIncreaseBid = new BidRequestDto(1100100L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생해야 한다.
        assertThatThrownBy(() -> bidService.createBid(productId, bidderId, exactIncreaseBid))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("최소 1000원 이상");
    }

    @Test
    @DisplayName("입찰 현황 조회 성공 테스트")
    void t7() {
        // Given: 입찰 데이터가 있는 상품이 주어졌을 때
        Long productId = 1L;
        Long bidderId = 1L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(productId, bidderId, requestDto);

        // When: 입찰 현황을 조회하면
        RsData<BidCurrentResponseDto> rsData = bidService.getBidStatus(productId);

        // Then: 현재 상황이 올바르게 반환되어야 한다.
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.msg()).isEqualTo("입찰 현황 조회 성공");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().productId()).isEqualTo(productId);
        assertThat(rsData.data().currentPrice()).isEqualTo(1100000L);
        assertThat(rsData.data().bidCount()).isGreaterThan(0);
        assertThat(rsData.data().recentBids()).isNotNull();
    }

    @Test
    @DisplayName("입찰 현황 조회 실패 테스트 - 존재하지 않는 상품")
    void t8() {
        // Given: 존재하지 않는 상품 ID가 주어졌을 때
        Long nonExistentProductId = 999L;

        // When & Then: 입찰 현황을 조회하면 ServiceException이 발생해야 한다.
        assertThatThrownBy(() -> bidService.getBidStatus(nonExistentProductId))
                .isInstanceOf(ServiceException.class)
                .hasMessage("404:존재하지 않는 상품입니다.");
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 테스트")
    void t9() {
        // Given: 입찰 데이터가 있는 사용자가 주어졌을 때
        Long memberId = 1L;
        Long productId = 1L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(productId, memberId, requestDto);

        // When: 내 입찰 내역을 조회하면
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(memberId, 0, 10);

        // Then: 내 입찰 내역이 올바르게 반환되어야 한다.
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.msg()).isEqualTo("내 입찰 내역 조회 성공");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().content()).isNotEmpty();
        assertThat(rsData.data().content().get(0).myBidPrice()).isEqualTo(1100000L);
        assertThat(rsData.data().content().get(0).isWinning()).isNotNull();
        assertThat(rsData.data().currentPage()).isEqualTo(0);
        assertThat(rsData.data().pageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 테스트 - 빈 결과")
    void t10() {
        // Given: 입찰 내역이 없는 사용자가 주어졌을 때
        Long memberIdWithNoBids = 999L;

        // When: 내 입찰 내역을 조회하면
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(memberIdWithNoBids, 0, 10);

        // Then: 빈 결과가 올바르게 반환되어야 한다.
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.msg()).isEqualTo("내 입찰 내역 조회 성공");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().content()).isEmpty();
        assertThat(rsData.data().totalElements()).isEqualTo(0);
        assertThat(rsData.data().hasNext()).isFalse();
    }

    @Test
    @DisplayName("내 입찰 내역 조회 - isWinning 플래그 정확성 테스트")
    void t11() {
        // Given: 여러 사용자의 입찰이 있는 상황에서
        Long productId = 1L;
        Long member1 = 1L;
        Long member2 = 2L;
        
        // 사용자1이 먼저 입찰
        BidRequestDto firstBid = new BidRequestDto(1100000L);
        bidService.createBid(productId, member1, firstBid);
        
        // 사용자2가 더 높게 입찰 (사용자1을 밀어냄)
        BidRequestDto higherBid = new BidRequestDto(1200000L);
        bidService.createBid(productId, member2, higherBid);

        // When: 사용자1의 입찰 내역을 조회하면
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(member1, 0, 10);

        // Then: isWinning이 false여야 한다.
        assertThat(rsData.data().content()).isNotEmpty();
        assertThat(rsData.data().content().get(0).isWinning()).isFalse();
        assertThat(rsData.data().content().get(0).myBidPrice()).isEqualTo(1100000L);
        assertThat(rsData.data().content().get(0).currentPrice()).isEqualTo(1200000L);
    }
}
