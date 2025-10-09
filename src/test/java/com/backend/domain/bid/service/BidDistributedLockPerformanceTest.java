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
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 분산락 성능 및 정합성 검증 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestElasticsearchConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BidDistributedLockPerformanceTest {

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

    private Product testProduct;
    private List<Member> testBidders;

    @BeforeEach
    void setUp() {
        Member seller = Member.builder()
                .email("perf_seller@test.com")
                .password("password")
                .nickname("성능테스트판매자")
                .phoneNumber("01012345678")
                .address("서울시 강남구")
                .authority("ROLE_USER")
                .build();
        memberRepository.save(seller);

        testBidders = java.util.stream.IntStream.range(1, 101)
                .mapToObj(i -> Member.builder()
                        .email("perf_bidder" + i + "@test.com")
                        .password("password")
                        .nickname("성능테스트입찰자" + i)
                        .phoneNumber("0101234567" + i)
                        .address("서울시 강남구")
                        .authority("ROLE_USER")
                        .build())
                .toList();
        memberRepository.saveAll(testBidders);

        testProduct = Product.testBuilder()
                .productName("성능테스트상품")
                .description("분산락 성능 테스트용 상품")
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
        productRepository.save(testProduct);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        try {
            // 외래키 제약조건을 고려한 삭제 순서
            
            // 1. Bid 삭제 (product_id 참조)
            bidRepository.deleteAll();
            entityManager.flush();
            
            // 2. Notification 삭제 (product_id 참조)
            entityManager.createQuery("DELETE FROM Notification").executeUpdate();
            entityManager.flush();
            
            // 3. Product 삭제
            productRepository.deleteAll();
            entityManager.flush();
            
            // 4. Member 삭제
            memberRepository.deleteAll();
            entityManager.flush();
            
            entityManager.clear();
        } catch (Exception e) {
            System.err.println("tearDown 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("100명 동시 입찰 - 데이터 정합성 검증")
    void test_100_concurrent_bids_data_consistency() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            final Long bidPrice = 1000000L + (i + 1) * 100L;
            
            executorService.submit(() -> {
                try {
                    Member bidder = testBidders.get(index);
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
                        System.out.println("입찰 실패: " + result.msg());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        long endTime = System.currentTimeMillis();

        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        List<Bid> allBids = bidRepository.findAll();
        Long highestBid = bidRepository.findHighestBidPrice(testProduct.getId()).orElse(0L);

        System.out.println("\n============ 100명 동시 입찰 테스트 결과 ============");
        System.out.println("총 요청 수: " + numberOfThreads);
        System.out.println("성공: " + successCount.get() + " | 실패: " + failCount.get());
        System.out.println("DB 저장 수: " + allBids.size());
        System.out.println("최종 가격: " + finalProduct.getCurrentPrice() + "원");
        System.out.println("DB 최고가: " + highestBid + "원");
        System.out.println("처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("TPS: " + String.format("%.2f", successCount.get() / ((endTime - startTime) / 1000.0)));
        System.out.println("정합성: " + (finalProduct.getCurrentPrice().equals(highestBid) && 
                allBids.size() == successCount.get() ? "✓" : "✗"));
        System.out.println("====================================================\n");

        // 정합성 검증
        assertThat(allBids.size()).as("DB에 저장된 입찰 수는 성공 카운트와 같아야 함").isEqualTo(successCount.get());
        assertThat(finalProduct.getCurrentPrice()).as("상품의 현재가는 최고 입찰가와 같아야 함").isEqualTo(highestBid);
        
        // 동시성 제어 검증 - 중복 입찰가가 없어야 함
        long distinctPrices = allBids.stream().map(Bid::getBidPrice).distinct().count();
        assertThat(distinctPrices).as("모든 입찰가는 고유해야 함 (동시성 제어 검증)").isEqualTo(allBids.size());
    }

    @Test
    @DisplayName("순차 처리 검증 - 입찰 가격이 계속 증가하는지 확인")
    void test_sequential_price_increase() throws InterruptedException {
        int numberOfThreads = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            final Long bidPrice = 1000000L + (i + 1) * 1000L;
            
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    
                    Member bidder = testBidders.get(index);
                    BidRequestDto request = new BidRequestDto(bidPrice);
                    bidService.createBid(testProduct.getId(), bidder.getId(), request);
                } catch (Exception e) {
                    // 입찰 실패
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        List<Bid> sortedBids = bidRepository.findAll().stream()
                .sorted((b1, b2) -> b1.getCreateDate().compareTo(b2.getCreateDate()))
                .toList();

        System.out.println("\n============ 순차 처리 검증 결과 ============");
        System.out.println("입찰 수: " + sortedBids.size());
        
        boolean isSequential = true;
        for (int i = 1; i < sortedBids.size(); i++) {
            if (sortedBids.get(i).getBidPrice() <= sortedBids.get(i - 1).getBidPrice()) {
                isSequential = false;
                break;
            }
        }
        
        System.out.println("순차 증가 여부: " + (isSequential ? "✓ YES" : "✗ NO"));
        System.out.println("최초 입찰가: " + (sortedBids.isEmpty() ? "N/A" : sortedBids.get(0).getBidPrice()) + "원");
        System.out.println("최종 입찰가: " + (sortedBids.isEmpty() ? "N/A" : sortedBids.get(sortedBids.size() - 1).getBidPrice()) + "원");
        System.out.println("===========================================\n");

        for (int i = 1; i < sortedBids.size(); i++) {
            assertThat(sortedBids.get(i).getBidPrice())
                    .isGreaterThan(sortedBids.get(i - 1).getBidPrice());
        }
    }
}
