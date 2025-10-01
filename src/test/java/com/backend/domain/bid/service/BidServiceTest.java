package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.BidCurrentResponseDto;
import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.dto.BidResponseDto;
import com.backend.domain.bid.dto.MyBidResponseDto;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestElasticsearchConfiguration.class)
class BidServiceTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

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

    private Product getActiveProduct() {
        // TestInitData에서 생성된 첫 번째 상품
        List<Product> products = productRepository.findAll();
        return products.stream()
                .filter(p -> p.getProductName().contains("iPhone 15 Pro"))
                .findFirst()
                .orElse(products.get(0)); // 첫 번째 상품
    }

    private Product getProductWithBids() {
        // TestInitData에서 생성된 입찰이 있는 상품
        List<Product> products = productRepository.findAll();
        return products.stream()
                .filter(p -> p.getProductName().contains("아이폰 15 Pro 256GB"))
                .findFirst()
                .orElse(products.get(3));
    }

    @Test
    @DisplayName("입찰 등록 성공 테스트")
    void t1() {
        // Given: TestInitData의 입찰자와 활성 상품
        Member bidder = getBidder1();
        Product product = getActiveProduct();
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When: 입찰을 시도
        RsData<BidResponseDto> rsData = bidService.createBid(product.getId(), bidder.getId(), requestDto);

        // Then: 성공 응답을 받고, 데이터베이스에 입찰이 저장
        assertThat(rsData.resultCode()).isEqualTo("201");
        assertThat(rsData.msg()).isEqualTo("입찰이 완료되었습니다.");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().productId()).isEqualTo(product.getId());
        assertThat(rsData.data().bidderId()).isEqualTo(bidder.getId());
        assertThat(rsData.data().price()).isEqualTo(1100000L);
        assertThat(rsData.data().status()).isEqualTo("bidding");

        // 실제 저장되었는지
        assertThat(bidRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 존재하지 않는 상품")
    void t2() {
        // Given: 존재하지 않는 상품 ID와 TestInitData의 입찰자
        Long nonExistentProductId = 999L;
        Member bidder = getBidder1();
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(nonExistentProductId, bidder.getId(), requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).isEqualTo("404:존재하지 않는 상품입니다.");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 존재하지 않는 사용자")
    void t3() {
        // Given: TestInitData의 상품과 존재하지 않는 사용자 ID
        Product product = getActiveProduct();
        Long nonExistentBidderId = 999L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(product.getId(), nonExistentBidderId, requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).isEqualTo("404:존재하지 않는 사용자입니다.");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 현재 최고가보다 낮은 금액")
    void t4() {
        // Given: TestInitData에서 이미 입찰이 있는 상품 사용
        Product product = getProductWithBids();
        Member bidder = getBidder1();
        
        // 현재 최고가보다 낮은 금액으로 입찰 시도
        BidRequestDto lowBidRequest = new BidRequestDto(1000000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(product.getId(), bidder.getId(), lowBidRequest);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).contains("현재 최고가");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 100원 단위가 아닌 금액")
    void t5() {
        // Given: TestInitData의 입찰자와 상품, 100원 단위가 아닌 입찰 금액
        Member bidder = getBidder1();
        Product product = getActiveProduct();
        BidRequestDto requestDto = new BidRequestDto(1100050L); // 500원 단위

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(product.getId(), bidder.getId(), requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).contains("100원 단위");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 최소 증가 금액 미만")
    void t6() {
        // Given: TestInitData의 입찰자와 상품
        Member bidder = getBidder1();
        Product product = getActiveProduct();
        
        // 먼저 입찰
        BidRequestDto firstBid = new BidRequestDto(1100000L);
        bidService.createBid(product.getId(), bidder.getId(), firstBid);
        
        // 100원 미만으로 높게 입찰 시도
        BidRequestDto lowIncreaseBid = new BidRequestDto(1100050L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(product.getId(), bidder.getId(), lowIncreaseBid);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).contains("100원 단위");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 본인 상품에 입찰")
    void t7() {
        // Given: TestInitData의 판매자와 그가 등록한 상품
        Member seller = getSeller();
        Product product = getActiveProduct(); // 판매자가 등록한 상품
        BidRequestDto requestDto = new BidRequestDto(1500000L);

        // When & Then: 본인 상품에 입찰하면 ServiceException이 발생
        try {
            bidService.createBid(product.getId(), seller.getId(), requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).isEqualTo("400:본인이 등록한 상품에는 입찰할 수 없습니다.");
        }
    }

    @Test
    @DisplayName("입찰 현황 조회 성공 테스트")
    void t8() {
        // Given: TestInitData의 입찰자와 상품
        Member bidder = getBidder1();
        Product product = getActiveProduct();
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(product.getId(), bidder.getId(), requestDto);

        // When: 입찰 현황을 조회
        RsData<BidCurrentResponseDto> rsData = bidService.getBidStatus(product.getId());

        // Then: 현재 상황이 올바르게 반환
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.msg()).isEqualTo("입찰 현황이 조회되었습니다.");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().productId()).isEqualTo(product.getId());
        assertThat(rsData.data().currentPrice()).isEqualTo(1100000L);
        assertThat(rsData.data().bidCount()).isGreaterThan(0);
        assertThat(rsData.data().recentBids()).isNotNull();
    }

    @Test
    @DisplayName("입찰 현황 조회 실패 테스트 - 존재하지 않는 상품")
    void t9() {
        // Given: 존재하지 않는 상품 ID
        Long nonExistentProductId = 999L;

        // When & Then: 입찰 현황을 조회하면 ServiceException이 발생
        try {
            bidService.getBidStatus(nonExistentProductId);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).isEqualTo("404:존재하지 않는 상품입니다.");
        }
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 테스트")
    void t10() {
        // Given: TestInitData의 입찰자가 입찰한 상황
        Member bidder = getBidder1();
        Product product = getActiveProduct();
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(product.getId(), bidder.getId(), requestDto);

        // When: 내 입찰 내역을 조회
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(bidder.getId(), 0, 10);

        // Then: 내 입찰 내역이 올바르게 반환
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.msg()).contains("내 입찰 내역");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().content()).isNotEmpty();
        assertThat(rsData.data().content().get(0).myBidPrice()).isEqualTo(1100000L);
        assertThat(rsData.data().content().get(0).isWinning()).isNotNull();
        assertThat(rsData.data().currentPage()).isEqualTo(0);
        assertThat(rsData.data().pageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공 테스트 - 빈 결과")
    void t11() {
        // Given: 입찰 내역이 없는 사용자 (판매자)
        Member seller = getSeller();

        // When: 내 입찰 내역을 조회
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(seller.getId(), 0, 10);

        // Then: 빈 결과가 올바르게 반환
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.msg()).contains("빈 입찰내역");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().content()).isEmpty();
        assertThat(rsData.data().totalElements()).isEqualTo(0);
        assertThat(rsData.data().hasNext()).isFalse();
    }

    @Test
    @DisplayName("내 입찰 내역 조회 - isWinning 플래그 정확성 테스트")
    void t12() {
        // Given: TestInitData의 두 입찰자가 경쟁하는 상황
        Member bidder1 = getBidder1();
        Member bidder2 = getBidder2();
        Product product = getActiveProduct();
        
        // 입찰자1이 먼저 입찰
        BidRequestDto firstBid = new BidRequestDto(1100000L);
        bidService.createBid(product.getId(), bidder1.getId(), firstBid);
        
        // 입찰자2가 더 높게 입찰
        BidRequestDto higherBid = new BidRequestDto(1200000L);
        bidService.createBid(product.getId(), bidder2.getId(), higherBid);

        // When: 입찰자1의 입찰 내역을 조회
        RsData<MyBidResponseDto> rsData1 = bidService.getMyBids(bidder1.getId(), 0, 10);

        // Then: isWinning이 false
        assertThat(rsData1.data().content()).isNotEmpty();
        assertThat(rsData1.data().content().get(0).isWinning()).isFalse();
        assertThat(rsData1.data().content().get(0).myBidPrice()).isEqualTo(1100000L);

        // When: 입찰자2의 입찰 내역을 조회
        RsData<MyBidResponseDto> rsData2 = bidService.getMyBids(bidder2.getId(), 0, 10);

        // Then: isWinning이 true
        assertThat(rsData2.data().content()).isNotEmpty();
        assertThat(rsData2.data().content().get(0).isWinning()).isTrue();
        assertThat(rsData2.data().content().get(0).myBidPrice()).isEqualTo(1200000L);
    }

    @Test
    @DisplayName("TestInitData에서 기존 입찰 데이터 확인 테스트")
    void t13() {
        // Given: TestInitData에서 이미 입찰이 있는 상품
        Product productWithBids = getProductWithBids();

        // When: 입찰 현황을 조회
        RsData<BidCurrentResponseDto> rsData = bidService.getBidStatus(productWithBids.getId());

        // Then: 기존 입찰 데이터가 있어야
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().bidCount()).isGreaterThan(0);
        assertThat(rsData.data().currentPrice()).isGreaterThan(rsData.data().initialPrice());
        assertThat(rsData.data().recentBids()).isNotEmpty();
    }

    @Test
    @DisplayName("TestInitData 입찰자의 기존 입찰 내역 확인 테스트")
    void t14() {
        // Given: TestInitData에서 이미 입찰한 입찰자1
        Member bidder1 = getBidder1();

        // When: 내 입찰 내역을 조회
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(bidder1.getId(), 0, 10);

        // Then: 기존 입찰 내역이 있을 수 있음 (TestInitData에 따라)
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.data()).isNotNull();
        // TestInitData에 입찰이 있으면 내역이 있고, 없으면 빈 배열
        assertThat(rsData.data().content()).isNotNull();
    }
}
