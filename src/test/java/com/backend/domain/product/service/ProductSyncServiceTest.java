package com.backend.domain.product.service;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSyncServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSearchService productSearchService;

    @InjectMocks
    private ProductSyncService productSyncService;

    @Test
    @DisplayName("상품 생성 시 Elasticsearch 동기화")
    void syncProductCreation() {
        // given
        Product product = createTestProduct(1L);

        doNothing().when(productSearchService).indexProduct(any(ProductDocument.class));

        // when
        productSyncService.syncProductCreation(product);

        // then
        verify(productSearchService, times(1)).indexProduct(any(ProductDocument.class));
    }

    @Test
    @DisplayName("상품 생성 동기화 실패 시 예외를 로그만 남기고 전파하지 않음")
    void syncProductCreation_withException() {
        // given
        Product product = createTestProduct(1L);

        doThrow(new RuntimeException("Elasticsearch error"))
                .when(productSearchService).indexProduct(any(ProductDocument.class));

        // when & then (예외가 전파되지 않아야 함)
        productSyncService.syncProductCreation(product);

        verify(productSearchService, times(1)).indexProduct(any(ProductDocument.class));
    }

    @Test
    @DisplayName("상품 수정 시 Elasticsearch 동기화")
    void syncProductUpdate() {
        // given
        Product product = createTestProduct(1L);

        doNothing().when(productSearchService).indexProduct(any(ProductDocument.class));

        // when
        productSyncService.syncProductUpdate(product);

        // then
        verify(productSearchService, times(1)).indexProduct(any(ProductDocument.class));
    }

    @Test
    @DisplayName("상품 삭제 시 Elasticsearch 동기화")
    void syncProductDeletion() {
        // given
        Long productId = 1L;

        doNothing().when(productSearchService).deleteProduct(String.valueOf(productId));

        // when
        productSyncService.syncProductDeletion(productId);

        // then
        verify(productSearchService, times(1)).deleteProduct(String.valueOf(productId));
    }

    @Test
    @DisplayName("전체 상품 재인덱싱 - 성공")
    void reindexAllProducts() {
        // given
        List<Product> products = List.of(
                createTestProduct(1L),
                createTestProduct(2L),
                createTestProduct(3L)
        );
        Page<Product> productPage = new PageImpl<>(products);

        when(productRepository.count()).thenReturn(3L);
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(productPage);
        doNothing().when(productSearchService).indexProduct(any(ProductDocument.class));

        // when
        productSyncService.reindexAllProducts();

        // then
        verify(productRepository, times(1)).count();
        verify(productRepository, times(1)).findAll(any(PageRequest.class));
        verify(productSearchService, times(3)).indexProduct(any(ProductDocument.class));
    }

    @Test
    @DisplayName("전체 상품 재인덱싱 - 상품 없음")
    void reindexAllProducts_withNoProducts() {
        // given
        when(productRepository.count()).thenReturn(0L);

        // when
        productSyncService.reindexAllProducts();

        // then
        verify(productRepository, times(1)).count();
        verify(productRepository, never()).findAll(any(PageRequest.class));
        verify(productSearchService, never()).indexProduct(any(ProductDocument.class));
    }

    @Test
    @DisplayName("전체 상품 재인덱싱 - 일부 실패")
    void reindexAllProducts_withPartialFailure() {
        // given
        List<Product> products = List.of(
                createTestProduct(1L),
                createTestProduct(2L),
                createTestProduct(3L)
        );
        Page<Product> productPage = new PageImpl<>(products);

        when(productRepository.count()).thenReturn(3L);
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(productPage);

        // 두 번째 상품 인덱싱 시 예외 발생
        doNothing()
                .doThrow(new RuntimeException("Indexing failed"))
                .doNothing()
                .when(productSearchService).indexProduct(any(ProductDocument.class));

        // when
        productSyncService.reindexAllProducts();

        // then
        verify(productRepository, times(1)).count();
        verify(productRepository, times(1)).findAll(any(PageRequest.class));
        verify(productSearchService, times(3)).indexProduct(any(ProductDocument.class));
    }

    @Test
    @DisplayName("전체 상품 재인덱싱 - 대용량 데이터 (여러 페이지)")
    void reindexAllProducts_withMultiplePages() {
        // given
        when(productRepository.count()).thenReturn(250L);

        when(productRepository.findAll(any(PageRequest.class))).thenAnswer(invocation -> {
            PageRequest pageRequest = invocation.getArgument(0);
            int pageNumber = pageRequest.getPageNumber();

            if (pageNumber == 0) {
                return new PageImpl<>(List.of(createTestProduct(1L), createTestProduct(2L)), pageRequest, 250);
            } else if (pageNumber == 1) {
                return new PageImpl<>(List.of(createTestProduct(3L)), pageRequest, 250 );
            } else {
                return new PageImpl<>(List.of(), pageRequest, 250);
            }
        });

        doNothing().when(productSearchService).indexProduct(any());

        // when
        productSyncService.reindexAllProducts();

        // then
        verify(productRepository, times(1)).count();
        verify(productRepository, times(3)).findAll(any(PageRequest.class));
        verify(productSearchService, times(3)).indexProduct(any(ProductDocument.class));
    }

    private Product createTestProduct(Long id) {
        Member seller = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("판매자")
                .build();

        return Product.testBuilder()
                .id(id)
                .productName("테스트 상품 " + id)
                .description("설명")
                .category(ProductCategory.DIGITAL_ELECTRONICS)
                .initialPrice(100000L)
                .currentPrice(100000L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .status(AuctionStatus.BIDDING.getDisplayName())
                .deliveryMethod(DeliveryMethod.BOTH)
                .location("서울")
                .seller(seller)
                .testBuild();
    }
}