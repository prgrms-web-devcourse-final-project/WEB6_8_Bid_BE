package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.repository.elasticsearch.ProductElasticRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductElasticRepository productElasticRepository;

    @InjectMocks
    private ProductSearchService productSearchService;

    @Test
    @DisplayName("상품 검색 - 정상 동작")
    void searchProducts() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto("아이폰", null, null, null, AuctionStatus.BIDDING);
        List<ProductDocument> documents = List.of(
                ProductDocument.builder().productId(1L).productName("아이폰").build()
        );
        Page<ProductDocument> expectedPage = new PageImpl<>(documents);

        when(productElasticRepository.searchProducts(any(Pageable.class), eq(searchDto)))
                .thenReturn(expectedPage);

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(
                1, 20, ProductSearchSortType.LATEST, searchDto
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("아이폰");
        verify(productElasticRepository, times(1)).searchProducts(any(Pageable.class), eq(searchDto));
    }

    @Test
    @DisplayName("상품 검색 - 페이지 번호 보정 (0 이하)")
    void searchProducts_withInvalidPage() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Page<ProductDocument> expectedPage = new PageImpl<>(List.of());

        when(productElasticRepository.searchProducts(any(Pageable.class), eq(searchDto)))
                .thenReturn(expectedPage);

        // when
        productSearchService.searchProducts(0, 20, ProductSearchSortType.LATEST, searchDto);

        // then
        verify(productElasticRepository).searchProducts(
                eq(PageRequest.of(0, 20, ProductSearchSortType.LATEST.toSort())),
                eq(searchDto)
        );
    }

    @Test
    @DisplayName("상품 검색 - 페이지 사이즈 보정 (100 초과)")
    void searchProducts_withInvalidSize() {
        // given
        ProductSearchDto searchDto = new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING);
        Page<ProductDocument> expectedPage = new PageImpl<>(List.of());

        when(productElasticRepository.searchProducts(any(Pageable.class), eq(searchDto)))
                .thenReturn(expectedPage);

        // when
        productSearchService.searchProducts(1, 150, ProductSearchSortType.LATEST, searchDto);

        // then
        verify(productElasticRepository).searchProducts(
                eq(PageRequest.of(0, 20, ProductSearchSortType.LATEST.toSort())),
                eq(searchDto)
        );
    }

    @Test
    @DisplayName("상품 인덱싱")
    void indexProduct() {
        // given
        ProductDocument document = ProductDocument.builder()
                .productId(1L)
                .productName("테스트 상품")
                .build();

        when(productElasticRepository.save(document)).thenReturn(document);

        // when
        productSearchService.indexProduct(document);

        // then
        verify(productElasticRepository, times(1)).save(document);
    }

    @Test
    @DisplayName("상품 삭제")
    void deleteProduct() {
        // given
        String productId = "1";

        doNothing().when(productElasticRepository).deleteById(productId);

        // when
        productSearchService.deleteProduct(productId);

        // then
        verify(productElasticRepository, times(1)).deleteById(productId);
    }

    @Test
    @DisplayName("회원별 상품 검색 - 정상 동작")
    void searchProductsByMember() {
        // given
        Member actor = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("판매자")
                .build();

        List<ProductDocument> documents = List.of(
                ProductDocument.builder()
                        .productId(1L)
                        .productName("상품1")
                        .sellerId(1L)
                        .build()
        );
        Page<ProductDocument> expectedPage = new PageImpl<>(documents);

        when(productElasticRepository.searchProductsByMember(any(Pageable.class), eq(1L), eq(SaleStatus.SELLING)))
                .thenReturn(expectedPage);

        // when
        Page<ProductDocument> result = productSearchService.searchProductsByMember(
                1, 20, ProductSearchSortType.LATEST, actor, SaleStatus.SELLING
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSellerId()).isEqualTo(1L);
        verify(productElasticRepository, times(1)).searchProductsByMember(
                any(Pageable.class), eq(1L), eq(SaleStatus.SELLING)
        );
    }

    @Test
    @DisplayName("회원별 상품 검색 - 모든 상태 조회")
    void searchProductsByMemberWithAllStatus() {
        // given
        Member actor = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("판매자")
                .build();

        Page<ProductDocument> expectedPage = new PageImpl<>(List.of());

        when(productElasticRepository.searchProductsByMember(any(Pageable.class), eq(1L), eq(null)))
                .thenReturn(expectedPage);

        // when
        productSearchService.searchProductsByMember(1, 20, ProductSearchSortType.LATEST, actor, null);

        // then
        verify(productElasticRepository).searchProductsByMember(
                any(Pageable.class), eq(1L), eq(null)
        );
    }

    @Test
    @DisplayName("회원별 상품 검색 - 페이지 번호 보정")
    void searchProductsByMember_withInvalidPage() {
        // given
        Member actor = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("판매자")
                .build();

        Page<ProductDocument> expectedPage = new PageImpl<>(List.of());

        when(productElasticRepository.searchProductsByMember(any(Pageable.class), eq(1L), eq(null)))
                .thenReturn(expectedPage);

        // when
        productSearchService.searchProductsByMember(0, 20, ProductSearchSortType.LATEST, actor, null);

        // then
        verify(productElasticRepository).searchProductsByMember(
                eq(PageRequest.of(0, 20, ProductSearchSortType.LATEST.toSort())),
                eq(1L),
                eq(null)
        );
    }

    @Test
    @DisplayName("회원별 상품 검색 - 페이지 사이즈 보정")
    void searchProductsByMember_withInvalidSize() {
        // given
        Member actor = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("판매자")
                .build();

        Page<ProductDocument> expectedPage = new PageImpl<>(List.of());

        when(productElasticRepository.searchProductsByMember(any(Pageable.class), eq(1L), eq(null)))
                .thenReturn(expectedPage);

        // when
        productSearchService.searchProductsByMember(1, 150, ProductSearchSortType.LATEST, actor, null);

        // then
        verify(productElasticRepository).searchProductsByMember(
                eq(PageRequest.of(0, 20, ProductSearchSortType.LATEST.toSort())),
                eq(1L),
                eq(null)
        );
    }
}