package com.backend.domain.product.repository;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.repository.elasticsearch.ProductElasticRepository;
import com.backend.domain.product.service.ProductSyncService;
import com.backend.global.redis.TestRedisConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.elasticsearch.uris=localhost:9200",
        "spring.data.elasticsearch.repositories.enabled=true",
        "spring.autoconfigure.exclude="  // 빈 값으로 오버라이드
})
@Import(TestRedisConfiguration.class)
class ProductElasticRepositoryTest {
    @Autowired
    private ProductElasticRepository productElasticRepository;

    @Autowired
    private ProductSyncService productSyncService;

    @BeforeEach
    void setUp() {
        // Elasticsearch 인덱스 초기화
        productElasticRepository.deleteAll();

        // 테스트 데이터 재인덱싱
        productSyncService.indexAllProducts();
    }

    @Test
    @DisplayName("키워드로 상품 검색")
    void searchByKeyword() {
        // given
        ProductSearchDto search = new ProductSearchDto("아이폰", null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.LATEST.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(item -> item.getProductName().contains("아이폰") || item.getProductName().contains("iPhone"));
    }

    @Test
    @DisplayName("카테고리로 상품 필터링")
    void searchByCategory() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, new Integer[]{ProductCategory.DIGITAL_ELECTRONICS.getId()}, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.LATEST.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent())
                .allMatch(doc -> doc.getCategory().equals(ProductCategory.DIGITAL_ELECTRONICS.name()));
    }

    @Test
    @DisplayName("지역으로 상품 필터링")
    void searchByLocation() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, new String[]{"서울"}, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.LATEST.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent())
                .allMatch(doc -> doc.getLocation().contains("서울"));
    }

    @Test
    @DisplayName("배송 가능 상품만 필터링")
    void searchByDelivery() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, null, true, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.LATEST.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent())
                .allMatch(doc -> doc.getDeliveryMethod().equals(DeliveryMethod.DELIVERY.name())
                        || doc.getDeliveryMethod().equals(DeliveryMethod.BOTH.name()));
    }

    @Test
    @DisplayName("복합 조건 검색")
    void searchWithMultipleConditions() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, new Integer[]{ProductCategory.DIGITAL_ELECTRONICS.getId()}, new String[]{"서울"}, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.LATEST.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent())
                .allMatch(doc -> doc.getCategory().equals(ProductCategory.DIGITAL_ELECTRONICS.name()))
                .allMatch(doc -> doc.getLocation().contains("서울"));
    }

    @Test
    @DisplayName("페이징 처리")
    void searchWithPaging() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 2, ProductSearchSortType.LATEST.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getTotalPages()).isEqualTo(4);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("정렬 - 최신순")
    void searchWithLatestSort() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.LATEST.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).isNotEmpty();

        // createDate 내림차순 확인
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            assertThat(result.getContent().get(i).getCreateDate())
                    .isAfterOrEqualTo(result.getContent().get(i + 1).getCreateDate());
            if (result.getContent().get(i).getCreateDate().equals(result.getContent().get(i + 1).getCreateDate())) {
                assertThat(result.getContent().get(i).getProductId())
                        .isGreaterThan(result.getContent().get(i + 1).getProductId());
            }
        }
    }

    @Test
    @DisplayName("정렬 - 가격 낮은순")
    void searchWithPriceLowSort() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.PRICE_LOW.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).isNotEmpty();

        // currentPrice 오름차순 확인
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            assertThat(result.getContent().get(i).getCurrentPrice())
                    .isLessThanOrEqualTo(result.getContent().get(i + 1).getCurrentPrice());
            if (result.getContent().get(i).getCurrentPrice().equals(result.getContent().get(i + 1).getCurrentPrice())) {
                assertThat(result.getContent().get(i).getProductId())
                        .isGreaterThan(result.getContent().get(i + 1).getProductId());
            }
        }
    }

    @Test
    @DisplayName("정렬 - 가격 높은순")
    void searchWithPriceHighSort() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.PRICE_HIGH.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).isNotEmpty();

        // currentPrice 내림차순 확인
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            assertThat(result.getContent().get(i).getCurrentPrice())
                    .isGreaterThanOrEqualTo(result.getContent().get(i + 1).getCurrentPrice());
            if (result.getContent().get(i).getCurrentPrice().equals(result.getContent().get(i + 1).getCurrentPrice())) {
                assertThat(result.getContent().get(i).getProductId())
                        .isGreaterThan(result.getContent().get(i + 1).getProductId());
            }
        }
    }

    @Test
    @DisplayName("정렬 - 마감 임박순")
    void searchWithEndingSoonSort() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.ENDING_SOON.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).isNotEmpty();

        // endTime 오름차순 확인
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            assertThat(result.getContent().get(i).getEndTime())
                    .isBeforeOrEqualTo(result.getContent().get(i + 1).getEndTime());
            if (result.getContent().get(i).getEndTime().equals(result.getContent().get(i + 1).getEndTime())) {
                assertThat(result.getContent().get(i).getProductId())
                        .isGreaterThan(result.getContent().get(i + 1).getProductId());
            }
        }
    }

    @Test
    @DisplayName("정렬 - 인기순")
    void searchWithPopularSort() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.POPULAR.toSort());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).isNotEmpty();

        // bidderCount 내림차순 확인
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            assertThat(result.getContent().get(i).getBidderCount())
                    .isGreaterThanOrEqualTo(result.getContent().get(i + 1).getBidderCount());
            if (result.getContent().get(i).getBidderCount().equals(result.getContent().get(i + 1).getBidderCount())) {
                assertThat(result.getContent().get(i).getProductId())
                        .isGreaterThan(result.getContent().get(i + 1).getProductId());
            }
        }
    }

    @Test
    @DisplayName("정렬 - Elasticsearch score 기준")
    void searchWithDefaultSort() {
        // given
        ProductSearchDto search = new ProductSearchDto("아이폰", null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(result.getContent().get(1).getProductName()).isEqualTo("아이폰 15 Pro 256GB");
    }
}