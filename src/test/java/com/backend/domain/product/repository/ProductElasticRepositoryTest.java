package com.backend.domain.product.repository;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
class ProductElasticRepositoryTest {
    @Autowired
    private ProductElasticRepository productElasticRepository;

    @Test
    @DisplayName("키워드로 상품 검색")
    void searchByKeyword() {
        // given
        ProductSearchDto search = new ProductSearchDto("아이폰", null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).contains("아이폰");
    }

    @Test
    @DisplayName("카테고리로 상품 필터링")
    void searchByCategory() {
        // given
        ProductSearchDto search = new ProductSearchDto(null, new Integer[]{ProductCategory.DIGITAL_ELECTRONICS.getId()}, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

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
        Pageable pageable = PageRequest.of(0, 10);

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
        Pageable pageable = PageRequest.of(0, 10);

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
        Pageable pageable = PageRequest.of(0, 10);

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
        Pageable pageable = PageRequest.of(0, 2);

        // when
        Page<ProductDocument> result = productElasticRepository.searchProducts(pageable, search);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getTotalPages()).isEqualTo(4);
        assertThat(result.hasNext()).isTrue();
    }
}