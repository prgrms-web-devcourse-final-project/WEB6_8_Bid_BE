package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.dto.BidResponseDto;
import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.redis.TestRedisConfiguration;
import com.backend.global.response.RsData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 분산락을 이용한 입찰 동시성 제어 테스트
 * 
 * Feature: 입찰 동시성 테스트 with Redisson 분산락
 * 
 * Scenario 1: 100명이 동시에 같은 상품에 입찰할 때, 순차적으로 처리되어야 함
 * Scenario 2: 서로 다른 상품에 대한 입찰은 동시에 처리 가능해야 함
 * Scenario 3: 락 타임아웃 시나리오 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestElasticsearchConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BidConcurrencyTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Product testProduct;
    private Product testProduct2;
    private List<Member> testBidders;

    @BeforeEach
    void setUp() {
        // 테스트용 판매자 생성
        Member seller = Member.builder()
                .email("seller@test.com")
                .password("password")
                .nickname("테스트판매자")
                .phoneNumber("01012345678")
                .address("서울시 강남구")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(seller);

        // 테스트용 입찰자들 생성 (100명)
        testBidders = java.util.stream.IntStream.range(1, 101)
                .mapToObj(i -> Member.builder()
                        .email("bidder" + i + "@test.com")
                        .password("password")
                        .nickname("테스트입찰자" + i)
                        .phoneNumber("0101234567" + i)
                        .address("서울시 강남구")
                        .authority("ROLE_USER")
                        .build())
                .toList();
        memberRepository.saveAll(testBidders);

        // 테스트용 상품 생성
        testProduct = Product.testBuilder()
                .productName("동시성테스트상품")
                .description("분산락 테스트용 상품")
                .category(ProductCategory.DIGITAL_ELECTRONICS)
                .initialPrice(1000000L)
                .currentPrice(1000000L)
                .status(AuctionStatus.BIDDING.getDisplayName())
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(7))
                .duration(192) // 8일 = 192시간
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .location("서울시")
                .seller(seller)
                .testBuild();
        productRepository.save(testProduct);

        // 두 번째 테스트용 상품 생성 (독립성 테스트용)
        testProduct2 = Product.testBuilder()
                .productName("동시성테스트상품2")
                .description("분산락 독립성 테스트용 상품")
                .category(ProductCategory.DIGITAL_ELECTRONICS)
                .initialPrice(1000000L)
                .currentPrice(1000000L)
                .status(AuctionStatus.BIDDING.getDisplayName())
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(7))
                .duration(192)
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .location("서울시")
                .seller(seller)
                .testBuild();
        productRepository.save(testProduct2);
    }

    @AfterEach
    void tearDown() {
        // @DirtiesContext를 사용하므로 수동 정리 불필요
        // 각 테스트 후 ApplicationContext가 자동으로 재생성됨
    }

    @Test
    @DisplayName("분산락 적용 - 100명이 동시에 입찰해도 순차적으로 처리")
    void concurrency_test_with_distributed_lock() throws InterruptedException {
        // Given: 100명의 입찰자가 동시에 입찰 준비
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명이 동시에 입찰 (각각 100원씩 증가하도록)
        for (int i = 0; i < numberOfThreads; i++) {
            final int bidderIndex = i;
            final Long bidPrice = 1000000L + (i + 1) * 100L; // 1,000,100원부터 시작
            
            executorService.submit(() -> {
                try {
                    Member bidder = testBidders.get(bidderIndex);
                    BidRequestDto request = new BidRequestDto(bidPrice);
                    
                    RsData<BidResponseDto> result = bidService.createBid(
                            testProduct.getId(), 
                            bidder.getId(), 
                            request
                    );
                    
                    if ("201".equals(result.resultCode())) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 데이터 정합성 검증
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        List<Bid> allBids = bidRepository.findAll();
        
        System.out.println("=== 분산락 동시성 테스트 결과 ===");
        System.out.println("총 요청 수: " + numberOfThreads);
        System.out.println("성공한 입찰 수: " + successCount.get());
        System.out.println("실패한 입찰 수: " + failCount.get());
        System.out.println("실제 저장된 입찰 수: " + allBids.size());
        System.out.println("최종 상품 가격: " + updatedProduct.getCurrentPrice());
        System.out.println("================================");

        // 검증: 성공한 입찰 수만큼 DB에 저장되어야 함
        assertThat(allBids.size()).isEqualTo(successCount.get());
        
        // 검증: 최종 가격은 최고 입찰가와 일치해야 함
        Long highestBidPrice = bidRepository.findHighestBidPrice(testProduct.getId()).orElse(0L);
        assertThat(updatedProduct.getCurrentPrice()).isEqualTo(highestBidPrice);
        
        // 검증: 모든 입찰이 순차적으로 증가했는지 확인
        List<Bid> sortedBids = allBids.stream()
                .sorted((b1, b2) -> b1.getCreateDate().compareTo(b2.getCreateDate()))
                .toList();
        
        for (int i = 1; i < sortedBids.size(); i++) {
            Long prevPrice = sortedBids.get(i - 1).getBidPrice();
            Long currentPrice = sortedBids.get(i).getBidPrice();
            assertThat(currentPrice).isGreaterThan(prevPrice);
        }
    }

    @Test
    @DisplayName("분산락 독립성 - 서로 다른 상품에 대한 입찰은 동시에 처리 가능")
    void concurrency_test_different_products() throws InterruptedException {
        // Given: 50명씩 서로 다른 상품에 입찰 준비
        int numberOfThreads = 100; // 상품1에 50명, 상품2에 50명
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        // When: 50명씩 각각 다른 상품에 동시 입찰
        for (int i = 0; i < numberOfThreads; i++) {
            final int bidderIndex = i;
            final Product targetProduct = (i < 50) ? testProduct : testProduct2;
            final Long bidPrice = 1000000L + (bidderIndex % 50 + 1) * 100L;
            
            executorService.submit(() -> {
                try {
                    Member bidder = testBidders.get(bidderIndex);
                    BidRequestDto request = new BidRequestDto(bidPrice);
                    
                    RsData<BidResponseDto> result = bidService.createBid(
                            targetProduct.getId(), 
                            bidder.getId(), 
                            request
                    );
                    
                    if ("201".equals(result.resultCode())) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 입찰 실패 (가격 경쟁에서 밀림)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        long endTime = System.currentTimeMillis();

        // Then: 두 상품 모두 입찰이 정상적으로 처리되었는지 확인
        List<Bid> product1Bids = bidRepository.findAll().stream()
                .filter(bid -> bid.getProduct().getId().equals(testProduct.getId()))
                .toList();
        
        List<Bid> product2Bids = bidRepository.findAll().stream()
                .filter(bid -> bid.getProduct().getId().equals(testProduct2.getId()))
                .toList();

        System.out.println("=== 분산락 독립성 테스트 결과 ===");
        System.out.println("총 요청 수: " + numberOfThreads);
        System.out.println("성공한 입찰 수: " + successCount.get());
        System.out.println("상품1 입찰 수: " + product1Bids.size());
        System.out.println("상품2 입찰 수: " + product2Bids.size());
        System.out.println("처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("================================");

        // 검증: 두 상품 모두 입찰이 저장되어야 함
        assertThat(product1Bids.size()).isGreaterThan(0);
        assertThat(product2Bids.size()).isGreaterThan(0);
        
        // 검증: 각 상품의 최종 가격이 올바른지 확인
        Product updatedProduct1 = productRepository.findById(testProduct.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElseThrow();
        
        Long highestBid1 = bidRepository.findHighestBidPrice(testProduct.getId()).orElse(0L);
        Long highestBid2 = bidRepository.findHighestBidPrice(testProduct2.getId()).orElse(0L);
        
        assertThat(updatedProduct1.getCurrentPrice()).isEqualTo(highestBid1);
        assertThat(updatedProduct2.getCurrentPrice()).isEqualTo(highestBid2);
    }

    @Test
    @DisplayName("동일 입찰자가 연속으로 입찰 시 최고가만 유효")
    void concurrency_test_same_bidder_sequential() throws InterruptedException {
        // Given: 한 명의 입찰자가 10번 연속으로 입찰 시도
        int numberOfAttempts = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfAttempts);
        CountDownLatch latch = new CountDownLatch(numberOfAttempts);
        
        Member bidder = testBidders.get(0);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 동일 입찰자가 가격을 올려가며 연속 입찰
        for (int i = 0; i < numberOfAttempts; i++) {
            final Long bidPrice = 1000000L + (i + 1) * 1000L;
            
            executorService.submit(() -> {
                try {
                    BidRequestDto request = new BidRequestDto(bidPrice);
                    RsData<BidResponseDto> result = bidService.createBid(
                            testProduct.getId(), 
                            bidder.getId(), 
                            request
                    );
                    
                    if ("201".equals(result.resultCode())) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 입찰 실패
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 모든 입찰이 순차적으로 처리되어야 함
        List<Bid> bidderBids = bidRepository.findAll().stream()
                .filter(bid -> bid.getMember().getId().equals(bidder.getId()))
                .toList();

        System.out.println("=== 연속 입찰 테스트 결과 ===");
        System.out.println("시도 횟수: " + numberOfAttempts);
        System.out.println("성공한 입찰 수: " + successCount.get());
        System.out.println("실제 저장된 입찰 수: " + bidderBids.size());
        System.out.println("================================");

        // 검증: 성공한 입찰만큼 저장되어야 함
        assertThat(bidderBids.size()).isEqualTo(successCount.get());
        
        // 검증: 각 입찰은 이전보다 높아야 함
        List<Bid> sortedBids = bidderBids.stream()
                .sorted((b1, b2) -> b1.getCreateDate().compareTo(b2.getCreateDate()))
                .toList();
        
        for (int i = 1; i < sortedBids.size(); i++) {
            assertThat(sortedBids.get(i).getBidPrice())
                    .isGreaterThan(sortedBids.get(i - 1).getBidPrice());
        }
    }

    @Test
    @DisplayName("입찰 경쟁 시나리오 - 실제 경매와 유사한 상황")
    void realistic_auction_scenario() throws InterruptedException {
        // Given: 10명의 입찰자가 랜덤한 시간에 입찰
        int numberOfBidders = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfBidders);
        CountDownLatch latch = new CountDownLatch(numberOfBidders);
        
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 각 입찰자가 서로 다른 가격으로 입찰
        Long[] bidPrices = {
                1100000L, 1150000L, 1120000L, 1200000L, 1180000L,
                1250000L, 1230000L, 1300000L, 1280000L, 1350000L
        };

        for (int i = 0; i < numberOfBidders; i++) {
            final int bidderIndex = i;
            final Long bidPrice = bidPrices[i];
            
            executorService.submit(() -> {
                try {
                    // 실제 사용자처럼 약간의 지연 시뮬레이션
                    Thread.sleep((long) (Math.random() * 100));
                    
                    Member bidder = testBidders.get(bidderIndex);
                    BidRequestDto request = new BidRequestDto(bidPrice);
                    
                    RsData<BidResponseDto> result = bidService.createBid(
                            testProduct.getId(), 
                            bidder.getId(), 
                            request
                    );
                    
                    if ("201".equals(result.resultCode())) {
                        successCount.incrementAndGet();
                        System.out.println("입찰 성공 - " + bidder.getNickname() + ": " + bidPrice + "원");
                    }
                } catch (Exception e) {
                    System.out.println("입찰 실패 - 입찰자" + bidderIndex + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 최종 결과 검증
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        List<Bid> allBids = bidRepository.findAll();
        
        System.out.println("\n=== 실전 경매 시나리오 결과 ===");
        System.out.println("총 입찰자 수: " + numberOfBidders);
        System.out.println("성공한 입찰 수: " + successCount.get());
        System.out.println("최종 낙찰가: " + finalProduct.getCurrentPrice() + "원");
        System.out.println("================================");

        // 검증: 최종 가격은 성공한 입찰 중 최고가여야 함
        Long expectedHighestPrice = allBids.stream()
                .map(Bid::getBidPrice)
                .max(Long::compareTo)
                .orElse(0L);
        
        assertThat(finalProduct.getCurrentPrice()).isEqualTo(expectedHighestPrice);
        assertThat(successCount.get()).isGreaterThan(0);
    }
}
