package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.repository.ProductImageRepository;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private FileService fileService;

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private ProductService productService;

    // ======================================= createProduct 테스트 ======================================= //
    @Test
    @DisplayName("상품 생성 성공")
    void create_Success() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = createValidImages();

        Product mockProduct = createMockProduct(1L);
        given(mockProduct.getId()).willReturn(1L);
        given(productRepository.save(any(Product.class)))
                .willReturn(mockProduct);

        // when
        Product result = productService.createProduct(actor, request, images);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        // 검증
        verify(productRepository).save(any(Product.class));
        verify(productImageService).validateAndCreateImages(mockProduct, images);
    }

    @Test
    @DisplayName("상품 생성 실패 - 직거래 시 배송지 누락")
    void create_FailByMissingLocation() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = new ProductCreateRequest(
                "테스트 상품",
                "설명",
                1,
                1000L,
                LocalDateTime.now().plusDays(1),
                "24시간",
                DeliveryMethod.TRADE,
                null // 빈 배송지
        );
        List<MultipartFile> images = createValidImages();

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-1")
                .hasFieldOrPropertyWithValue("msg", "직거래 시 배송지는 필수입니다");
    }

    @Test
    @DisplayName("상품 생성 실패 - ProductImageService 에서 예외 발생")
    void create_FailByNoImages() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = new ArrayList<>(); // 빈 리스트

        Product mockProduct = createMockProduct(1L);
        given(productRepository.save(any(Product.class))).willReturn(mockProduct);

        // ProductImageService 에서 예외 발생 시뮬레이션
        doThrow(ProductException.imageRequired())
                .when(productImageService).validateAndCreateImages(mockProduct, images);

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2");

        verify(productRepository).save(any(Product.class));
        verify(productImageService).validateAndCreateImages(mockProduct, images);
    }

    @Test
    @DisplayName("배송지 검증 - 배송만 선택 시 배송지 생략 가능")
    void validateLocation_DeliveryOnlyWithoutLocation() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = new ProductCreateRequest(
                "테스트 상품",
                "설명",
                1,
                1000L,
                LocalDateTime.now().plusDays(1),
                "24시간",
                DeliveryMethod.DELIVERY,
                null // 배송지 없음
        );
        List<MultipartFile> images = createValidImages();

        Product mockProduct = createMockProduct(1L);
        given(productRepository.save(any(Product.class))).willReturn(mockProduct);

        // when & then - 예외가 발생하지 않아야 함
        assertThatCode(() -> productService.createProduct(actor, request, images))
                .doesNotThrowAnyException();

        verify(productRepository).save(any(Product.class));
        verify(productImageService).validateAndCreateImages(mockProduct, images);
    }

    @Test
    @DisplayName("배송지 검증 - 둘다 선택 시 배송지 필수")
    void validateLocation_BothWithLocation() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = new ProductCreateRequest(
                "테스트 상품",
                "설명",
                1,
                1000L,
                LocalDateTime.now().plusDays(1),
                "24시간",
                DeliveryMethod.BOTH,
                "서울특별시" // 배송지 있음
        );
        List<MultipartFile> images = createValidImages();

        Product mockProduct = createMockProduct(1L);
        given(productRepository.save(any(Product.class))).willReturn(mockProduct);

        // when & then - 예외가 발생하지 않아야 함
        assertThatCode(() -> productService.createProduct(actor, request, images))
                .doesNotThrowAnyException();

        verify(productRepository).save(any(Product.class));
        verify(productImageService).validateAndCreateImages(mockProduct, images);
    }

    // ======================================= modifyProduct 테스트 ======================================= //
    @Test
    @DisplayName("상품 수정 성공 - 이미지 추가")
    void modifyProduct_Success() {
        // given
        Product mockProduct = mock(Product.class);
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = createValidImages();
        List<Long> deleteImageIds = List.of(1L, 2L);

        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        given(mockProduct.getProductName()).willReturn("다른 이름");
        given(mockProduct.getDescription()).willReturn("다른 설명");

        // when
        Product result = productService.modifyProduct(mockProduct, request, images, deleteImageIds);

        // then
        assertThat(result).isNotNull();

        // ProductService의 책임만 검증
        verify(mockProduct).modify(any(ProductModifyRequest.class));
        verify(productImageService).validateAndModifyImages(mockProduct, images, deleteImageIds);
    }

    @Test
    @DisplayName("상품 수정 성공 - 기본 정보만 수정")
    void modifyProduct_SuccessWithOnlyBasicInfo() {
        // given
        Product mockProduct = mock(Product.class);
        ProductModifyRequest request = createValidModifyRequest();

        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        given(mockProduct.getProductName()).willReturn("다른 이름");

        // when
        Product result = productService.modifyProduct(mockProduct, request, null, null);

        // then
        assertThat(result).isNotNull();

        verify(mockProduct).modify(any(ProductModifyRequest.class));
        verify(productImageService).validateAndModifyImages(mockProduct, null, null);
    }

    @Test
    @DisplayName("상품 수정 실패 - 직거래 시 배송지 누락")
    void modifyProduct_FailByMissingLocation() {
        // given
        Product mockProduct = mock(Product.class);
        ProductModifyRequest request = new ProductModifyRequest(
                "수정된 상품명",
                "수정된 설명",
                2,
                2000L,
                LocalDateTime.now().plusDays(2),
                "48시간",
                DeliveryMethod.TRADE,
                null // 배송지 없음
        );

        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(2));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, null, null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-1")
                .hasFieldOrPropertyWithValue("msg", "직거래 시 배송지는 필수입니다");
    }

    @Test
    @DisplayName("상품 수정 검증 실패 - 경매 시작 시간이 지난 상품")
    void validateModifyRequest_FailByAuctionStarted() {
        // given
        Product mockProduct = mock(Product.class);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().minusHours(1)); // 1시간 전 시작

        ProductModifyRequest request = createValidModifyRequest();

        // when & then
        assertThatThrownBy(() -> productService.validateModifyRequest(mockProduct, request))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "403-3")
                .hasFieldOrPropertyWithValue("msg", "경매 시작 시간이 지났으므로 상품 수정이 불가능합니다");
    }

    @Test
    @DisplayName("상품 수정 실패 - ProductImageService 에서 예외 발생")
    void modifyProduct_FailByImageServiceException() {
        // given
        Product mockProduct = mock(Product.class);
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = createValidImages();

        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        given(mockProduct.getProductName()).willReturn("다른 이름");

        // ProductImageService 에서 예외 발생
        doThrow(ProductException.imageMaxCountExceeded())
                .when(productImageService).validateAndModifyImages(mockProduct, images, null);

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, images, null))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-3");

        verify(mockProduct).modify(any(ProductModifyRequest.class));
        verify(productImageService).validateAndModifyImages(mockProduct, images, null);
    }

    @Test
    @DisplayName("상품 수정 검증 - 변경되지 않은 필드들은 null로 반환")
    void validateModifyRequest_NullForUnchangedFields() {
        // given
        String existingName = "기존 상품명";
        String existingDescription = "기존 설명";

        Product mockProduct = mock(Product.class);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        given(mockProduct.getProductName()).willReturn(existingName);
        given(mockProduct.getDescription()).willReturn(existingDescription);
        given(mockProduct.getCategory()).willReturn(ProductCategory.DIGITAL_ELECTRONICS);
        given(mockProduct.getInitialPrice()).willReturn(1000L);
        given(mockProduct.getDeliveryMethod()).willReturn(DeliveryMethod.DELIVERY);
        given(mockProduct.getLocation()).willReturn("서울");

        // 동일한 값으로 요청
        ProductModifyRequest request = new ProductModifyRequest(
                existingName, // 동일한 이름
                existingDescription, // 동일한 설명
                1, // 동일한 카테고리
                1000L, // 동일한 가격
                LocalDateTime.now().plusDays(1),
                "24시간",
                DeliveryMethod.DELIVERY, // 동일한 배송 방법
                "서울" // 동일한 위치
        );

        // when
        ProductModifyRequest result = productService.validateModifyRequest(mockProduct, request);

        // then - 변경되지 않은 필드들은 null
        assertThat(result.name()).isNull();
        assertThat(result.description()).isNull();
        assertThat(result.categoryId()).isNull();
        assertThat(result.initialPrice()).isNull();
        assertThat(result.deliveryMethod()).isNull();
        assertThat(result.location()).isNull();
    }

    // ======================================= deleteProduct 테스트 ======================================= //
    @Test
    @DisplayName("상품 삭제 성공")
    void deleteProduct_Success() {
        // given
        Product mockProduct = mock(Product.class);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1)); // 경매 시작 전

        // when
        productService.deleteProduct(mockProduct);

        // then
        verify(productImageService).deleteAllProductImages(mockProduct);
        verify(productRepository).delete(mockProduct);
    }

    @Test
    @DisplayName("상품 삭제 실패 - 경매 시작 후")
    void deleteProduct_FailByAuctionStarted() {
        // given
        Product mockProduct = mock(Product.class);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().minusHours(1)); // 경매 이미 시작

        // when & then
        assertThatThrownBy(() -> productService.deleteProduct(mockProduct))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "403-4")
                .hasFieldOrPropertyWithValue("msg", "경매 시작 시간이 지났으므로 상품 삭제가 불가능합니다");

        // 어떤 삭제 작업도 수행되지 않아야 함
        verifyNoInteractions(productImageService);
        verify(productRepository, never()).delete(any());
    }


    // ======================================= Helper methods for create tests  ======================================= //
    private Member createMockMember() {
        return mock(Member.class);
    }

    private ProductCreateRequest createValidRequest() {
        return new ProductCreateRequest(
                "테스트 상품",
                "상품 설명",
                1, // categoryId
                1000L, // initialPrice
                LocalDateTime.now().plusDays(1),
                "24시간", // auctionDuration
                DeliveryMethod.DELIVERY,
                "서울특별시"
        );
    }

    private List<MultipartFile> createValidImages() {
        return List.of(
                new MockMultipartFile("image1", "test1.jpg", "image/jpeg", "content1".getBytes()),
                new MockMultipartFile("image2", "test2.png", "image/png", "content2".getBytes())
        );
    }

    private List<MultipartFile> createTooManyImages() {
        List<MultipartFile> images = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            images.add(new MockMultipartFile("image" + i, "test" + i + ".jpg", "image/jpeg", ("content" + i).getBytes()));
        }
        return images;
    }

    private Product createMockProduct(Long id) {
        return mock(Product.class);
    }

    // ======================================= Helper methods for modify tests  ======================================= //
    private ProductModifyRequest createValidModifyRequest() {
        return new ProductModifyRequest(
                "수정된 상품명",
                "수정된 설명",
                1,
                2000L,
                LocalDateTime.now().plusDays(2),
                "48시간",
                DeliveryMethod.BOTH,
                "부산광역시"
        );
    }
}