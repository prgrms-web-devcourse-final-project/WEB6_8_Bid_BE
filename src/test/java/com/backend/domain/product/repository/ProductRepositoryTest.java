package com.backend.domain.product.repository;

import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
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

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestElasticsearchConfiguration.class)
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
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .contains("아이폰 15 Pro 256GB", "갤럭시 S24 Ultra 512GB");
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
        assertThat(result.getContent()).hasSize(4);
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
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .containsExactlyInAnyOrder("나이키 Air Max", "갤럭시 S24 Ultra 512GB", "MacBook Pro M3", "iPhone 15 Pro");
        assertThat(result.getContent())
                .allMatch(product -> product.getDeliveryMethod() == DeliveryMethod.DELIVERY || product.getDeliveryMethod() == DeliveryMethod.BOTH);
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
        assertThat(result.getContent()).hasSize(3);
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
        assertThat(products).hasSize(7);

        for (int i = 0; i < products.size() - 1; i++) {
            assertThat(products.get(i).getCurrentPrice() <= products.get(i+1).getCurrentPrice()).isTrue();
        }
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
        assertThat(products).hasSize(7);

        for (int i = 0; i < products.size() - 1; i++) {
            assertThat(products.get(i).getCurrentPrice() >= products.get(i+1).getCurrentPrice()).isTrue();
        }
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
        assertThat(products).hasSize(7);

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
        assertThat(products).hasSize(7);

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
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getTotalPages()).isEqualTo(4);
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
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getTotalPages()).isEqualTo(4);
        assertThat(result.hasNext()).isTrue();
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
        assertThat(result.getContent()).hasSize(4);

        for (int i = 0; i < result.getContent().size(); i++) {
            assertThat(result.getContent().get(i).getDeliveryMethod()).isIn(DeliveryMethod.TRADE, DeliveryMethod.BOTH);
        }
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
        assertThat(result.getContent()).hasSize(7);
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .contains(
                        "아이폰 15 Pro 256GB",
                        "갤럭시 S24 Ultra 512GB",
                        "구찌 GG 마몽 숄더백",
                        "나이키 Air Max"
                );
    }

    @Test
    @DisplayName("특정 회원의 모든 상품을 조회할 수 있다")
    void findByMemberAll() {
        // given
        Long sellerId = 5L; // 실제 테스트 데이터의 판매자 ID
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findByMemberPaged(pageable, sellerId, null);

        // then
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent())
                .allMatch(product -> product.getSeller().getId().equals(sellerId));
    }

    @Test
    @DisplayName("특정 회원의 경매중인 상품만 조회할 수 있다")
    void findByMemberWithBiddingStatus() {
        // given
        Long sellerId = 5L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findByMemberPaged(pageable, sellerId, AuctionStatus.BIDDING);

        // then
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent())
                .allMatch(product -> product.getSeller().getId().equals(sellerId))
                .allMatch(product -> product.getStatus().equals(AuctionStatus.BIDDING.getDisplayName()));
    }

    @Test
    @DisplayName("특정 회원의 판매완료 상품만 조회할 수 있다")
    void findByMemberWithSoldStatus() {
        // given
        Long sellerId = 4L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findByMemberPaged(pageable, sellerId, AuctionStatus.SUCCESSFUL);

        // then
        // 판매완료 상품이 없다면 빈 결과 반환
        assertThat(result.getContent())
                .allMatch(product -> product.getSeller().getId().equals(sellerId))
                .allMatch(product -> product.getStatus().equals(AuctionStatus.SUCCESSFUL.getDisplayName()));
    }

    @Test
    @DisplayName("특정 회원의 유찰 상품만 조회할 수 있다")
    void findByMemberWithFailedStatus() {
        // given
        Long sellerId = 3L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findByMemberPaged(pageable, sellerId, AuctionStatus.FAILED);

        // then
        assertThat(result.getContent())
                .allMatch(product -> product.getSeller().getId().equals(sellerId))
                .allMatch(product -> product.getStatus().equals(AuctionStatus.FAILED.getDisplayName()));
    }

    @Test
    @DisplayName("회원별 상품 조회에서 페이징이 정상적으로 동작한다")
    void findByMemberWithPaging() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 2); // 첫 번째 페이지, 2개씩

        // when
        Page<Product> result = productRepository.findByMemberPaged(pageable, sellerId, null);

        // then
        assertThat(result.getContent()).hasSizeLessThanOrEqualTo(2);
        assertThat(result.getContent())
                .allMatch(product -> product.getSeller().getId().equals(sellerId));
    }

    @Test
    @DisplayName("회원별 상품 조회에서 기본 정렬은 최신순이다")
    void findByMemberWithDefaultSorting() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10, ProductSearchSortType.LATEST.toSort());

        // when
        Page<Product> result = productRepository.findByMemberPaged(pageable, sellerId, null);

        // then
        List<Product> products = result.getContent();
        if (products.size() > 1) {
            // createDate 내림차순으로 정렬되어야 함
            for (int i = 0; i < products.size() - 1; i++) {
                assertThat(products.get(i).getCreateDate())
                        .isAfterOrEqualTo(products.get(i + 1).getCreateDate());
            }
        }
    }

    @Test
    @DisplayName("회원별 상품 조회 - 복합 조건 테스트")
    void findByMemberComplexCondition() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> biddingProducts = productRepository.findByMemberPaged(pageable, sellerId, AuctionStatus.BIDDING);
        Page<Product> allProducts = productRepository.findByMemberPaged(pageable, sellerId, null);

        // then
        assertThat(biddingProducts.getTotalElements()).isLessThanOrEqualTo(allProducts.getTotalElements());
        assertThat(biddingProducts.getContent())
                .allMatch(product -> product.getSeller().getId().equals(sellerId))
                .allMatch(product -> product.getStatus().equals(AuctionStatus.BIDDING.getDisplayName()));
    }
}