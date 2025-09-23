package com.backend.domain.product.repository;

import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("키워드로 상품을 검색할 수 있다 - 아이폰")
    void findByKeywordIphone() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto("아이폰", null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("아이폰 15 Pro 256GB");
    }

    @Test
    @DisplayName("전자기기 카테고리로 상품을 필터링할 수 있다")
    void findByElectronicsCategory() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, new Integer[]{1}, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .containsExactlyInAnyOrder("아이폰 15 Pro 256GB", "갤럭시 S24 Ultra 512GB");
    }

    @Test
    @DisplayName("패션 카테고리로 상품을 필터링할 수 있다")
    void findByFashionCategory() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, new Integer[]{2}, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .containsExactlyInAnyOrder("구찌 GG 마몽 숄더백", "나이키 Air Max");
    }

    @Test
    @DisplayName("서울 지역으로 상품을 필터링할 수 있다")
    void findBySeoulLocation() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, new String[]{"서울"}, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("아이폰 15 Pro 256GB");
    }

    @Test
    @DisplayName("부산 지역으로 상품을 필터링할 수 있다")
    void findByBusanLocation() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, new String[]{"부산"}, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("구찌 GG 마몽 숄더백");
    }

    @Test
    @DisplayName("배송 가능 상품만 필터링할 수 있다")
    void findByDeliveryMethod() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, true, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .containsExactlyInAnyOrder("갤럭시 S24 Ultra 512GB", "나이키 Air Max");
        assertThat(result.getContent())
                .allMatch(product -> product.getDeliveryMethod() == DeliveryMethod.DELIVERY);
    }

    @Test
    @DisplayName("전자기기 + 배송가능 조건으로 검색할 수 있다")
    void findByElectronicsAndDelivery() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, new Integer[]{1}, null, true, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("갤럭시 S24 Ultra 512GB");
    }

    @Test
    @DisplayName("가격 오름차순으로 정렬할 수 있다")
    void findWithPriceAscending() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("currentPrice").ascending());

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        List<Product> products = result.getContent();
        assertThat(products).hasSize(4);

        // 가격 순서 확인: 나이키(700000) < 구찌(800000) < 갤럭시(1200000) < 아이폰(1300000)
        assertThat(products.get(0).getProductName()).isEqualTo("나이키 Air Max");
        assertThat(products.get(1).getProductName()).isEqualTo("구찌 GG 마몽 숄더백");
        assertThat(products.get(2).getProductName()).isEqualTo("갤럭시 S24 Ultra 512GB");
        assertThat(products.get(3).getProductName()).isEqualTo("아이폰 15 Pro 256GB");
    }

    @Test
    @DisplayName("가격 내림차순으로 정렬할 수 있다")
    void findWithPriceDescending() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("currentPrice").descending());

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        List<Product> products = result.getContent();
        assertThat(products).hasSize(4);

        // 가격 순서 확인: 아이폰(1300000) > 갤럭시(1200000) > 구찌(800000) > 나이키(700000)
        assertThat(products.get(0).getProductName()).isEqualTo("아이폰 15 Pro 256GB");
        assertThat(products.get(1).getProductName()).isEqualTo("갤럭시 S24 Ultra 512GB");
        assertThat(products.get(2).getProductName()).isEqualTo("구찌 GG 마몽 숄더백");
        assertThat(products.get(3).getProductName()).isEqualTo("나이키 Air Max");
    }

    @Test
    @DisplayName("인기순으로 정렬할 수 있다")
    void findWithBidderCountDescending() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("bidderCount").descending());

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        List<Product> products = result.getContent();
        assertThat(products).hasSize(4);

        // 아이폰에 입찰자가 2명이므로 첫 번째로 와야 함
        assertThat(products.get(0).getProductName()).isEqualTo("아이폰 15 Pro 256GB");
    }

    @Test
    @DisplayName("마감 임박순으로 정렬할 수 있다")
    void findWithEndTimeSorting() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("endTime").ascending());

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        List<Product> products = result.getContent();
        assertThat(products).hasSize(4);

        // 마감시간이 이른 순서대로 정렬되어야 함
        for (int i = 0; i < products.size() - 1; i++) {
            assertThat(products.get(i).getEndTime())
                    .isBeforeOrEqualTo(products.get(i + 1).getEndTime());
        }
    }

    @Test
    @DisplayName("페이징이 정상적으로 동작한다")
    void findWithPaging() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 2); // 첫 번째 페이지, 2개씩

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("두 번째 페이지를 조회할 수 있다")
    void findSecondPage() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 2개씩

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 키워드로 검색하면 빈 결과를 반환한다")
    void findByNonExistentKeyword() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto("존재하지않는상품", null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("복합 검색 조건 - 전자기기 + 서울 + 직거래/배송")
    void findByComplexConditions() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, new Integer[]{1}, new String[]{"서울"}, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("아이폰 15 Pro 256GB");
        assertThat(result.getContent().get(0).getDeliveryMethod()).isEqualTo(DeliveryMethod.TRADE);
    }

    @Test
    @DisplayName("모든 상품을 조회할 수 있다")
    void findAllProducts() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findBySearchPaged(pageable, searchDto);

        // then
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .containsExactlyInAnyOrder(
                        "아이폰 15 Pro 256GB",
                        "갤럭시 S24 Ultra 512GB",
                        "구찌 GG 마몽 숄더백",
                        "나이키 Air Max"
                );
    }
}