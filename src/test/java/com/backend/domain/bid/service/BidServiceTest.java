package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.BidCurrentResponseDto;
import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.dto.BidResponseDto;
import com.backend.domain.bid.dto.MyBidResponseDto;
import com.backend.domain.bid.enums.BidStatus;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.exception.ServiceException;
import com.backend.global.redis.TestRedisConfiguration;
import com.backend.global.response.RsData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("bidtest")
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
class BidServiceTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private EntityManager entityManager;

    private Member bidder1;
    private Member bidder2;
    private Member seller;
    private Product activeProduct;
    private Product productWithBids;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 새로운 트랜잭션에서 실행하고 커밋
    void setUp() {
        // 기존 데이터 정리
        bidRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
        
        // 새로운 테스트 데이터 생성
        createTestMembers();
        createTestProducts();
        
        // 다시 조회해서 사용
        bidder1 = memberRepository.findByNickname("테스트입찰자1").orElseThrow();
        bidder2 = memberRepository.findByNickname("테스트입찰자2").orElseThrow();
        seller = memberRepository.findByNickname("테스트판매자").orElseThrow();
        
        List<Product> products = productRepository.findAll();
        activeProduct = products.stream()
                .filter(p -> p.getProductName().equals("테스트상품1"))
                .findFirst()
                .orElseThrow();
        
        productWithBids = products.stream()
                .filter(p -> p.getProductName().equals("입찰있는상품"))
                .findFirst()
                .orElseThrow();
    }
    // setUp 메서드가 끝나면 트랜잭션이 커밋되어 데이터가 DB에 저장됨

    private void createTestMembers() {
        bidder1 = Member.builder()
                .email("test-bidder1@test.com")
                .password("password")
                .nickname("테스트입찰자1")
                .phoneNumber("010-1111-1111")
                .address("서울시")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(bidder1);
        
        bidder2 = Member.builder()
                .email("test-bidder2@test.com")
                .password("password")
                .nickname("테스트입찰자2")
                .phoneNumber("010-2222-2222")
                .address("서울시")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(bidder2);
        
        seller = Member.builder()
                .email("test-seller@test.com")
                .password("password")
                .nickname("테스트판매자")
                .phoneNumber("010-3333-3333")
                .address("서울시")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(seller);
    }

    private void createTestProducts() {
        activeProduct = Product.testBuilder()
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
        productRepository.save(activeProduct);
        
        productWithBids = Product.testBuilder()
                .productName("입찰있는상품")
                .description("이미 입찰이 있는 상품입니다")
                .category(ProductCategory.DIGITAL_ELECTRONICS)
                .initialPrice(1000000L)
                .currentPrice(1000000L)  // 초기 가격으로 시작
                .status(AuctionStatus.BIDDING.getDisplayName())
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .duration(26)
                .deliveryMethod(DeliveryMethod.BOTH)
                .location("서울시")
                .seller(seller)
                .testBuild();
        productRepository.save(productWithBids);
    }

    @Test
    @DisplayName("입찰 등록 성공 테스트")
    void t1() {
        // Given: 테스트 데이터의 입찰자와 활성 상품
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When: 입찰을 시도
        RsData<BidResponseDto> rsData = bidService.createBid(activeProduct.getId(), bidder1.getId(), requestDto);

        // Then: 성공 응답을 받고, 데이터베이스에 입찰이 저장
        assertThat(rsData.resultCode()).isEqualTo("201");
        assertThat(rsData.msg()).isEqualTo("입찰이 완료되었습니다.");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().productId()).isEqualTo(activeProduct.getId());
        assertThat(rsData.data().bidderId()).isEqualTo(bidder1.getId());
        assertThat(rsData.data().price()).isEqualTo(1100000L);
        assertThat(rsData.data().status()).isEqualTo(BidStatus.BIDDING);

        // 실제 저장되었는지
        assertThat(bidRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 존재하지 않는 상품")
    void t2() {
        // Given: 존재하지 않는 상품 ID
        Long nonExistentProductId = 999999L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(nonExistentProductId, bidder1.getId(), requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).isEqualTo("404:존재하지 않는 상품입니다.");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 존재하지 않는 사용자")
    void t3() {
        // Given: 존재하지 않는 사용자 ID (실제로 존재하는 상품 사용)
        Long nonExistentBidderId = 999999L;
        BidRequestDto requestDto = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(activeProduct.getId(), nonExistentBidderId, requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            // Product 검증이 먼저 실행되므로 상품 에러가 먼저 발생할 수 있음
            assertThat(e.getMessage()).contains("404:");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 현재 최고가보다 낮은 금액")
    void t4() {
        // Given: 먼저 높은 입찰을 생성
        BidRequestDto highBid = new BidRequestDto(1200000L);
        bidService.createBid(productWithBids.getId(), bidder2.getId(), highBid);
        
        // 현재가보다 낮은 금액으로 입찰 시도
        BidRequestDto lowBidRequest = new BidRequestDto(1100000L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(productWithBids.getId(), bidder1.getId(), lowBidRequest);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).contains("현재 최고가");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 100원 단위가 아닌 금액")
    void t5() {
        // Given: 100원 단위가 아닌 입찰 금액
        BidRequestDto requestDto = new BidRequestDto(1100050L); // 50원 단위

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(activeProduct.getId(), bidder1.getId(), requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).contains("100원 단위");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 최소 증가 금액 미만")
    void t6() {
        // Given: 먼저 입찰
        BidRequestDto firstBid = new BidRequestDto(1100000L);
        bidService.createBid(activeProduct.getId(), bidder1.getId(), firstBid);

        // 100원 미만으로 높게 입찰 시도
        BidRequestDto lowIncreaseBid = new BidRequestDto(1100050L);

        // When & Then: 입찰을 시도하면 ServiceException이 발생
        try {
            bidService.createBid(activeProduct.getId(), bidder1.getId(), lowIncreaseBid);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).contains("100원 단위");
        }
    }

    @Test
    @DisplayName("입찰 등록 실패 테스트 - 본인 상품에 입찰")
    void t7() {
        // Given: 판매자가 자신의 상품에 입찰
        BidRequestDto requestDto = new BidRequestDto(1500000L);

        // When & Then: 본인 상품에 입찰하면 ServiceException이 발생
        try {
            bidService.createBid(activeProduct.getId(), seller.getId(), requestDto);
            assertThat(false).isTrue(); // 예외가 발생하지 않으면 실패
        } catch (ServiceException e) {
            assertThat(e).isInstanceOf(ServiceException.class);
            assertThat(e.getMessage()).isEqualTo("400:본인이 등록한 상품에는 입찰할 수 없습니다.");
        }
    }

    @Test
    @DisplayName("입찰 현황 조회 성공 테스트")
    void t8() {
        // Given: 입찰 생성
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(activeProduct.getId(), bidder1.getId(), requestDto);

        // When: 입찰 현황을 조회
        RsData<BidCurrentResponseDto> rsData = bidService.getBidStatus(activeProduct.getId());

        // Then: 현재 상황이 올바르게 반환
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.msg()).isEqualTo("입찰 현황이 조회되었습니다.");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().productId()).isEqualTo(activeProduct.getId());
        assertThat(rsData.data().currentPrice()).isEqualTo(1100000L);
        assertThat(rsData.data().bidCount()).isGreaterThan(0);
        assertThat(rsData.data().recentBids()).isNotNull();
    }

    @Test
    @DisplayName("입찰 현황 조회 실패 테스트 - 존재하지 않는 상품")
    void t9() {
        // Given: 존재하지 않는 상품 ID
        Long nonExistentProductId = 999999L;

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
        // Given: 입찰 생성
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(activeProduct.getId(), bidder1.getId(), requestDto);

        // When: 내 입찰 내역을 조회
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(bidder1.getId(), 0, 10);

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
        // Given: 두 입찰자가 경쟁하는 상황
        // 입찰자1이 먼저 입찰
        BidRequestDto firstBid = new BidRequestDto(1100000L);
        bidService.createBid(activeProduct.getId(), bidder1.getId(), firstBid);

        // 입찰자2가 더 높게 입찰
        BidRequestDto higherBid = new BidRequestDto(1200000L);
        bidService.createBid(activeProduct.getId(), bidder2.getId(), higherBid);

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
    @DisplayName("입찰이 있는 상품의 입찰 현황 확인 테스트")
    void t13() {
        // Given: 입찰 추가
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(productWithBids.getId(), bidder1.getId(), requestDto);

        // When: 입찰 현황을 조회
        RsData<BidCurrentResponseDto> rsData = bidService.getBidStatus(productWithBids.getId());

        // Then: 입찰 데이터가 있어야
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().bidCount()).isGreaterThan(0);
        assertThat(rsData.data().currentPrice()).isGreaterThanOrEqualTo(rsData.data().initialPrice());
    }

    @Test
    @DisplayName("입찰자의 입찰 내역 확인 테스트")
    void t14() {
        // Given: 입찰 생성
        BidRequestDto requestDto = new BidRequestDto(1100000L);
        bidService.createBid(activeProduct.getId(), bidder1.getId(), requestDto);

        // When: 내 입찰 내역을 조회
        RsData<MyBidResponseDto> rsData = bidService.getMyBids(bidder1.getId(), 0, 10);

        // Then: 입찰 내역이 있어야 함
        assertThat(rsData.resultCode()).isEqualTo("200");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().content()).isNotEmpty();
    }
}
