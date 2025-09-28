package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
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
import java.util.Optional;

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
    void create_Success() {
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
                .hasFieldOrPropertyWithValue("msg", "직거래 시 배송지는 필수입니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 이미지 없음")
    void create_FailByNoImages() {
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
    void create_FailByTooManyImages() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = createTooManyImages(); // 6개

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-3")
                .hasFieldOrPropertyWithValue("msg", "이미지는 최대 5개까지만 업로드할 수 있습니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 빈 파일")
    void create_FailByEmptyFile() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]) // 빈 파일
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-4")
                .hasFieldOrPropertyWithValue("msg", "빈 파일은 업로드할 수 없습니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 파일 크기 초과")
    void create_FailByFileSizeExceeded() {
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
                .hasFieldOrPropertyWithValue("resultCode", "400-5")
                .hasFieldOrPropertyWithValue("msg", "이미지 파일 크기는 5MB를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 지원하지 않는 파일 형식")
    void create_FailByUnsupportedFileType() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-7")
                .hasFieldOrPropertyWithValue("msg", "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
    }

    @Test
    @DisplayName("상품 생성 실패 - 잘못된 파일명")
    void create_FailByInvalidFileName() {
        // given
        Member actor = createMockMember();
        ProductCreateRequest request = createValidRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", null, "image/jpeg", "content".getBytes()) // null 파일명
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(actor, request, images))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-6")
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

    @Test
    @DisplayName("상품 수정 성공 - 이미지 추가")
    void modifyProduct_SuccessWithAddImages() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = createValidImages();

        Product mockProduct = mock(Product.class);
        given(mockProduct.getId()).willReturn(productId);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/image3.jpg")
                .willReturn("http://localhost:8080/uploads/products/1/image4.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(new ProductImage("http://localhost:8080/uploads/products/1/image3.jpg", mockProduct))
                .willReturn(new ProductImage("http://localhost:8080/uploads/products/1/image4.jpg", mockProduct));

        // when
        Product result = productService.modifyProduct(mockProduct, request, images, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);

        verify(mockProduct).modify(request);
        verify(fileService, times(2)).uploadFile(any(MultipartFile.class), eq("products/1"));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("상품 수정 성공 - 이미지 삭제 (파일 삭제 포함)")
    void modifyProduct_SuccessWithFileService() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<Long> deleteImageIds = List.of(10L);

        Product mockProduct = mock(Product.class);
        given(mockProduct.getId()).willReturn(productId);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        ProductImage mockImage = mock(ProductImage.class);
        given(mockImage.getProduct()).willReturn(mockProduct);
        given(mockImage.getImageUrl()).willReturn("http://example.com/image.jpg");

        given(productImageRepository.findById(10L))
                .willReturn(Optional.of(mockImage));
        given(mockProduct.getProductImages())
                .willReturn(List.of(mock(ProductImage.class))); // 삭제 후에도 이미지 남음

        // when
        Product result = productService.modifyProduct(mockProduct, request, null, deleteImageIds);

        // then
        assertThat(result).isNotNull();

        verify(fileService).deleteFile("http://example.com/image.jpg"); // 파일 삭제 확인
        verify(mockProduct).deleteProductImage(mockImage);
        verify(productImageRepository).delete(mockImage);
    }

    @Test
    @DisplayName("상품 수정 성공 - 이미지 추가 및 삭제")
    void modifyProduct_SuccessWithAddAndDeleteImages() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = createValidImages();
        List<Long> deleteImageIds = List.of(10L);

        Product mockProduct = mock(Product.class);
        given(mockProduct.getId()).willReturn(productId); // ID 설정 추가
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        ProductImage mockImage = mock(ProductImage.class);
        ProductImage remainingImage = mock(ProductImage.class);

        given(mockImage.getProduct()).willReturn(mockProduct);

        given(productImageRepository.findById(10L))
                .willReturn(Optional.of(mockImage));
        given(mockProduct.getProductImages())
                .willReturn(List.of(remainingImage)); // 삭제 후에도 이미지가 남아있음
        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/new_image.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(new ProductImage("http://localhost:8080/uploads/products/1/new_image.jpg", mockProduct));

        // when
        Product result = productService.modifyProduct(mockProduct, request, images, deleteImageIds);

        // then
        assertThat(result).isNotNull();

        verify(mockProduct).modify(request);
        verify(fileService, times(2)).uploadFile(any(MultipartFile.class), eq("products/1"));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(mockProduct).deleteProductImage(mockImage);
        verify(productImageRepository).delete(mockImage);
    }

    @Test
    @DisplayName("상품 수정 실패 - 존재하지 않는 이미지 삭제")
    void modifyProduct_FailByImageNotFound() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<Long> deleteImageIds = List.of(999L); // 존재하지 않는 이미지 ID

        Product mockProduct = mock(Product.class);

        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        given(productImageRepository.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, null, deleteImageIds))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "404-2")
                .hasFieldOrPropertyWithValue("msg", "존재하지 않는 이미지입니다.");
    }

    @Test
    @DisplayName("상품 수정 실패 - 다른 상품의 이미지 삭제 시도")
    void modifyProduct_FailByImageNotBelongToProduct() {
        // given
        Long productId = 1L;
        Long otherProductId = 2L;
        ProductModifyRequest request = createValidModifyRequest();
        List<Long> deleteImageIds = List.of(10L);

        Product mockProduct = mock(Product.class);
        Product otherProduct = mock(Product.class);
        given(otherProduct.getId()).willReturn(otherProductId);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        ProductImage mockImage = mock(ProductImage.class);

        given(mockImage.getProduct()).willReturn(otherProduct);

        given(productImageRepository.findById(10L))
                .willReturn(Optional.of(mockImage));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, null, deleteImageIds))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-9")
                .hasFieldOrPropertyWithValue("msg", "이미지가 해당 상품에 속하지 않습니다.");
    }

    @Test
    @DisplayName("상품 수정 실패 - 모든 이미지 삭제로 인한 이미지 부족")
    void modifyProduct_FailByNoImagesLeft() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<Long> deleteImageIds = List.of(10L);

        Product mockProduct = mock(Product.class);
        given(mockProduct.getId()).willReturn(productId); // ID 설정 추가
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));
        ProductImage mockImage = mock(ProductImage.class);

        given(mockImage.getProduct()).willReturn(mockProduct);

        given(productImageRepository.findById(10L))
                .willReturn(Optional.of(mockImage));
        given(mockProduct.getProductImages())
                .willReturn(List.of()); // 삭제 후 이미지가 없음

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, null, deleteImageIds))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "이미지는 필수입니다.");
    }

    @Test
    @DisplayName("상품 수정 성공 - 기본 정보만 수정")
    void modifyProduct_SuccessWithOnlyBasicInfo() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();

        Product mockProduct = mock(Product.class);
        given(mockProduct.getId()).willReturn(productId);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        // when
        Product result = productService.modifyProduct(mockProduct, request, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);

        verify(mockProduct).modify(request);
        verifyNoInteractions(fileService);
        verify(productImageRepository, never()).save(any(ProductImage.class));
        verify(productImageRepository, never()).delete(any(ProductImage.class));
    }

    @Test
    @DisplayName("상품 수정 성공 - 빈 이미지 리스트")
    void modifyProduct_SuccessWithEmptyImageList() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = new ArrayList<>(); // 빈 리스트

        Product mockProduct = mock(Product.class);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        // when
        Product result = productService.modifyProduct(mockProduct, request, images, null);

        // then
        assertThat(result).isNotNull();

        verify(mockProduct).modify(request);
        verifyNoInteractions(fileService);
        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("상품 수정 실패 - 이미지 개수 초과 (기존 + 새로운)")
    void modifyProduct_FailByTooManyImagesWithExisting() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = createTooManyImages(); // 6개 이미지

        Product mockProduct = mock(Product.class);
        given(mockProduct.getProductImages()).willReturn(List.of(mock(ProductImage.class))); // 기존 이미지 1개
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, images, null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-3")
                .hasFieldOrPropertyWithValue("msg", "이미지는 최대 5개까지만 업로드할 수 있습니다.");
    }

    @Test
    @DisplayName("상품 수정 실패 - 빈 파일 업로드")
    void modifyProduct_FailByEmptyFile() {
        // given
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]) // 빈 파일
        );

        Product mockProduct = mock(Product.class);
        given(mockProduct.getProductImages()).willReturn(List.of(mock(ProductImage.class)));
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, images, null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-4")
                .hasFieldOrPropertyWithValue("msg", "빈 파일은 업로드할 수 없습니다.");
    }

    @Test
    @DisplayName("상품 수정 실패 - 파일 크기 초과")
    void modifyProduct_FailByFileSizeExceeded() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();

        // 6MB 파일 생성
        byte[] largeContent = new byte[6 * 1024 * 1024];
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image", "large.jpg", "image/jpeg", largeContent)
        );

        Product mockProduct = mock(Product.class);
        given(mockProduct.getProductImages()).willReturn(List.of(mock(ProductImage.class)));
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, images, null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-5")
                .hasFieldOrPropertyWithValue("msg", "이미지 파일 크기는 5MB를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("상품 수정 실패 - 지원하지 않는 파일 형식")
    void modifyProduct_FailByUnsupportedFileType() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes())
        );

        Product mockProduct = mock(Product.class);
        given(mockProduct.getProductImages()).willReturn(List.of(mock(ProductImage.class)));
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, images, null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-7")
                .hasFieldOrPropertyWithValue("msg", "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
    }

    @Test
    @DisplayName("상품 수정 실패 - 잘못된 파일명")
    void modifyProduct_FailByInvalidFileName() {
        // given
        Long productId = 1L;
        ProductModifyRequest request = createValidModifyRequest();
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", null, "image/jpeg", "content".getBytes()) // null 파일명
        );

        Product mockProduct = mock(Product.class);
        given(mockProduct.getProductImages()).willReturn(List.of(mock(ProductImage.class)));
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, images, null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-6")
                .hasFieldOrPropertyWithValue("msg", "올바른 파일명이 아닙니다.");
    }

    @Test
    @DisplayName("상품 수정 실패 - 직거래 시 배송지 누락")
    void modifyProduct_FailByMissingLocation() {
        // given
        Long productId = 1L;
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

        Product mockProduct = mock(Product.class);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(2));

        // when & then
        assertThatThrownBy(() -> productService.modifyProduct(mockProduct, request, null, null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-1")
                .hasFieldOrPropertyWithValue("msg", "직거래 시 배송지는 필수입니다.");
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
                .hasFieldOrPropertyWithValue("msg", "경매 시작 시간이 지났으므로 상품 수정이 불가능합니다.");
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

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteProduct_Success() {
        // given
        Product mockProduct = mock(Product.class);
        given(mockProduct.getStartTime()).willReturn(LocalDateTime.now().plusHours(1)); // 경매 시작 전

        ProductImage mockImage1 = mock(ProductImage.class);
        ProductImage mockImage2 = mock(ProductImage.class);
        given(mockImage1.getImageUrl()).willReturn("http://example.com/image1.jpg");
        given(mockImage2.getImageUrl()).willReturn("http://example.com/image2.jpg");
        given(mockProduct.getProductImages()).willReturn(List.of(mockImage1, mockImage2));

        // when
        productService.deleteProduct(mockProduct);

        // then
        verify(fileService).deleteFile("http://example.com/image1.jpg");
        verify(fileService).deleteFile("http://example.com/image2.jpg");
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
                .hasFieldOrPropertyWithValue("msg", "경매 시작 시간이 지났으므로 상품 삭제가 불가능합니다.");

        // 파일 삭제나 DB 삭제가 호출되지 않았는지 확인
        verifyNoInteractions(fileService);
        verify(productRepository, never()).delete(any(Product.class));
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
        Product product = mock(Product.class);
        given(product.getId()).willReturn(id);
        return product;
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