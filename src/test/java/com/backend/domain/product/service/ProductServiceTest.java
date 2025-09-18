package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.enums.DeliveryMethod;
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
import static org.mockito.ArgumentMatchers.*;
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

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_Success() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = createValidImages();

        Product mockProduct = createMockProduct(1L);
        given(productRepository.save(any(Product.class)))
                .willReturn(mockProduct);
        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/image1.jpg")
                .willReturn("http://localhost:8080/uploads/products/1/image2.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(new ProductImage("http://localhost:8080/uploads/products/1/image1.jpg", mockProduct))
                .willReturn(new ProductImage("http://localhost:8080/uploads/products/2/image1.jpg", mockProduct));

        // when
        Product result = productService.createProduct(actor, request, images);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        // 검증
        verify(productRepository).save(any(Product.class));
        verify(fileService, times(2)).uploadFile(any(MultipartFile.class), eq("products/1"));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("상품 생성 실패 - 직거래 시 배송지 누락")
    void createProduct_FailByMissingLocation() {
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
                .hasFieldOrPropertyWithValue("msg", "직거래 시 배송지는 필수입니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 이미지 없음")
    void createProduct_FailByNoImages() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = new ArrayList<>(); // 빈 리스트

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "이미지는 필수입니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 이미지 개수 초과")
    void createProduct_FailByTooManyImages() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = createTooManyImages(); // 6개

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "이미지는 최대 5개까지만 업로드할 수 있습니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 빈 파일")
    void createProduct_FailByEmptyFile() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]) // 빈 파일
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "빈 파일은 업로드할 수 없습니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 파일 크기 초과")
    void createProduct_FailByFileSizeExceeded() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();

        // 6MB 파일 생성
        byte[] largeContent = new byte[6 * 1024 * 1024];
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image", "large.jpg", "image/jpeg", largeContent)
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "이미지 파일 크기는 5MB를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 지원하지 않는 파일 형식")
    void createProduct_FailByUnsupportedFileType() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
    }

    @Test
    @DisplayName("상품 생성 실패 - 잘못된 파일명")
    void createProduct_FailByInvalidFileName() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", null, "image/jpeg", "content".getBytes()) // null 파일명
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "올바른 파일명이 아닙니다.");
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
        given(productRepository.save(any(Product.class)))
                .willReturn(mockProduct);
        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/image1.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(new ProductImage("http://localhost:8080/uploads/products/1/image1.jpg", mockProduct));

        // when & then - 예외가 발생하지 않아야 함
        assertThatCode(() -> productService.createProduct(actor, request, images))
                .doesNotThrowAnyException();
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
        given(productRepository.save(any(Product.class)))
                .willReturn(mockProduct);
        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/image1.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(new ProductImage("http://localhost:8080/uploads/products/1/image1.jpg", mockProduct));

        // when & then - 예외가 발생하지 않아야 함
        assertThatCode(() -> productService.createProduct(actor, request, images))
                .doesNotThrowAnyException();
    }

    // Helper methods
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
        Product product = mock(Product.class);
        given(product.getId()).willReturn(id);
        return product;
    }
}