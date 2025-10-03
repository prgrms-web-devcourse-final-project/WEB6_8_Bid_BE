package com.backend.domain.product.service;

import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.repository.ProductImageRepository;
import com.backend.global.file.service.LocalFileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private LocalFileService fileService;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductImageService productImageService;

    // ======================================= validateAndCreateImages 테스트 ======================================= //

    @Test
    @DisplayName("이미지 생성 성공")
    void validateAndCreateImages_Success() {
        // given
        Product mockProduct = createMockProduct(1L);
        given(mockProduct.getId()).willReturn(1L);
        List<MultipartFile> images = createValidImages();

        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/image1.jpg")
                .willReturn("http://localhost:8080/uploads/products/1/image2.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(mock(ProductImage.class));

        // when
        productImageService.validateAndCreateImages(mockProduct, images);

        // then
        verify(fileService, times(2)).uploadFile(any(MultipartFile.class), eq("products/1"));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(mockProduct, times(2)).addProductImage(any(ProductImage.class));
    }

    @Test
    @DisplayName("이미지 생성 실패 - 이미지 없음")
    void validateAndCreateImages_FailByNoImages() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> images = List.of();

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "이미지는 필수입니다");

        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 생성 실패 - null 이미지 리스트")
    void validateAndCreateImages_FailByNullImages() {
        // given
        Product mockProduct = createMockProduct(1L);

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, null))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "이미지는 필수입니다");

        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 생성 실패 - 이미지 개수 초과")
    void validateAndCreateImages_FailByTooManyImages() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> images = createTooManyImages(); // 6개

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-3")
                .hasFieldOrPropertyWithValue("msg", "이미지는 최대 5개까지만 업로드할 수 있습니다");

        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 생성 실패 - 빈 파일")
    void validateAndCreateImages_FailByEmptyFile() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0])
        );

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-4")
                .hasFieldOrPropertyWithValue("msg", "빈 파일은 업로드할 수 없습니다");

        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 생성 실패 - 파일 크기 초과")
    void validateAndCreateImages_FailByFileSizeExceeded() {
        // given
        Product mockProduct = createMockProduct(1L);
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image", "large.jpg", "image/jpeg", largeContent)
        );

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-5")
                .hasFieldOrPropertyWithValue("msg", "이미지 파일 크기는 5MB를 초과할 수 없습니다");

        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 생성 실패 - 잘못된 파일명")
    void validateAndCreateImages_FailByInvalidFileName() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", null, "image/jpeg", "content".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-6")
                .hasFieldOrPropertyWithValue("msg", "올바른 파일명이 아닙니다");
    }

    @Test
    @DisplayName("이미지 생성 실패 - 확장자 없는 파일명")
    void validateAndCreateImages_FailByNoExtension() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", "filename", "image/jpeg", "content".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-6")
                .hasFieldOrPropertyWithValue("msg", "올바른 파일명이 아닙니다");
    }

    @Test
    @DisplayName("이미지 생성 실패 - 지원하지 않는 파일 형식")
    void validateAndCreateImages_FailByUnsupportedFileType() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-7")
                .hasFieldOrPropertyWithValue("msg", "지원하지 않는 파일 형식입니다 (jpg, jpeg, png, gif, webp만 가능)");
    }

    @Test
    @DisplayName("이미지 생성 성공 - 지원하는 모든 파일 형식 테스트")
    void validateAndCreateImages_SuccessAllSupportedFormats() {
        // given
        Product mockProduct = createMockProduct(1L);
        given(mockProduct.getId()).willReturn(1L);
        List<MultipartFile> images = List.of(
                new MockMultipartFile("image1", "test.jpg", "image/jpeg", "content1".getBytes()),
                new MockMultipartFile("image2", "test.jpeg", "image/jpeg", "content2".getBytes()),
                new MockMultipartFile("image3", "test.png", "image/png", "content3".getBytes()),
                new MockMultipartFile("image4", "test.gif", "image/gif", "content4".getBytes()),
                new MockMultipartFile("image5", "test.webp", "image/webp", "content5".getBytes())
        );

        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/image.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(mock(ProductImage.class));

        // when & then
        assertThatCode(() -> productImageService.validateAndCreateImages(mockProduct, images))
                .doesNotThrowAnyException();

        verify(fileService, times(5)).uploadFile(any(MultipartFile.class), eq("products/1"));
        verify(productImageRepository, times(5)).save(any(ProductImage.class));
    }

    // ======================================= validateAndModifyImages 테스트 ======================================= //

    @Test
    @DisplayName("이미지 수정 성공 - 추가만")
    void validateAndModifyImages_SuccessAddOnly() {
        // given
        Product mockProduct = createMockProduct(1L);
        given(mockProduct.getId()).willReturn(1L);
        List<MultipartFile> newImages = createValidImages();
        given(mockProduct.getProductImages()).willReturn(List.of(mock(ProductImage.class))); // 기존 1개

        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/new_image.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(mock(ProductImage.class));

        // when
        productImageService.validateAndModifyImages(mockProduct, newImages, null);

        // then
        verify(fileService, times(2)).uploadFile(any(MultipartFile.class), eq("products/1"));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(productImageRepository).flush();
    }

    @Test
    @DisplayName("이미지 수정 성공 - 삭제만")
    void validateAndModifyImages_SuccessDeleteOnly() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<Long> deleteImageIds = List.of(1L);

        ProductImage mockImage = mock(ProductImage.class);
        given(mockImage.getProduct()).willReturn(mockProduct);
        given(mockImage.getImageUrl()).willReturn("https://example.com/image.jpg");
        given(productImageRepository.findById(1L)).willReturn(Optional.of(mockImage));
        given(mockProduct.getProductImages()).willReturn(List.of(mock(ProductImage.class))); // 삭제 후에도 1개 남음

        // when
        productImageService.validateAndModifyImages(mockProduct, null, deleteImageIds);

        // then
        verify(fileService).deleteFile("https://example.com/image.jpg");
        verify(mockProduct).deleteProductImage(mockImage);
        verify(productImageRepository).delete(mockImage);
    }

    @Test
    @DisplayName("이미지 수정 성공 - 추가와 삭제 동시에")
    void validateAndModifyImages_SuccessAddAndDelete() {
        // given
        Product mockProduct = createMockProduct(1L);
        given(mockProduct.getId()).willReturn(1L);
        List<MultipartFile> newImages = createValidImages();
        List<Long> deleteImageIds = List.of(1L);

        // 삭제할 이미지 Mock
        ProductImage mockImageToDelete = mock(ProductImage.class);
        given(mockImageToDelete.getProduct()).willReturn(mockProduct);
        given(mockImageToDelete.getImageUrl()).willReturn("https://example.com/old_image.jpg");
        given(productImageRepository.findById(1L)).willReturn(Optional.of(mockImageToDelete));

        // 기존 이미지 Mock (삭제 후에도 남을 이미지들)
        ProductImage remainingImage1 = mock(ProductImage.class);
        ProductImage remainingImage2 = mock(ProductImage.class);
        given(mockProduct.getProductImages()).willReturn(List.of(remainingImage1, remainingImage2));

        given(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://localhost:8080/uploads/products/1/new_image.jpg");
        given(productImageRepository.save(any(ProductImage.class)))
                .willReturn(mock(ProductImage.class));

        // when
        productImageService.validateAndModifyImages(mockProduct, newImages, deleteImageIds);

        // then
        // 삭제 검증
        verify(fileService).deleteFile("https://example.com/old_image.jpg");
        verify(mockProduct).deleteProductImage(mockImageToDelete);
        verify(productImageRepository).delete(mockImageToDelete);

        // 추가 검증
        verify(fileService, times(2)).uploadFile(any(MultipartFile.class), eq("products/1"));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(productImageRepository).flush();
    }

    @Test
    @DisplayName("이미지 수정 성공 - 아무것도 하지 않음")
    void validateAndModifyImages_SuccessDoNothing() {
        // given
        Product mockProduct = createMockProduct(1L);

        // when
        productImageService.validateAndModifyImages(mockProduct, null, null);

        // then
        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 수정 성공 - 빈 리스트들")
    void validateAndModifyImages_SuccessEmptyLists() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> emptyImages = List.of();
        List<Long> emptyDeleteIds = List.of();

        // when
        productImageService.validateAndModifyImages(mockProduct, emptyImages, emptyDeleteIds);

        // then
        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 수정 실패 - 기존 이미지와 합쳐서 개수 초과")
    void validateAndModifyImages_FailByTooManyImagesWithExisting() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<MultipartFile> newImages = createTooManyImages(); // 6개

        ProductImage existingImage = mock(ProductImage.class);
        given(mockProduct.getProductImages()).willReturn(List.of(existingImage)); // 기존 1개

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndModifyImages(mockProduct, newImages, null))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-3");

        verifyNoInteractions(fileService, productImageRepository);
    }

    @Test
    @DisplayName("이미지 수정 실패 - 존재하지 않는 이미지 삭제")
    void validateAndModifyImages_FailByImageNotFound() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<Long> deleteImageIds = List.of(999L);

        given(productImageRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndModifyImages(mockProduct, null, deleteImageIds))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "404-2")
                .hasFieldOrPropertyWithValue("msg", "존재하지 않는 이미지입니다");
    }

    @Test
    @DisplayName("이미지 수정 실패 - 다른 상품의 이미지 삭제 시도")
    void validateAndModifyImages_FailByImageNotBelongToProduct() {
        // given
        Product mockProduct = createMockProduct(1L);
        given(mockProduct.getId()).willReturn(1L);
        Product otherProduct = createMockProduct(2L);
        List<Long> deleteImageIds = List.of(1L);

        ProductImage mockImage = mock(ProductImage.class);
        given(mockImage.getProduct()).willReturn(otherProduct); // 다른 상품의 이미지
        given(productImageRepository.findById(1L)).willReturn(Optional.of(mockImage));

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndModifyImages(mockProduct, null, deleteImageIds))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-9")
                .hasFieldOrPropertyWithValue("msg", "이미지가 해당 상품에 속하지 않습니다");
    }

    @Test
    @DisplayName("이미지 수정 실패 - 모든 이미지 삭제로 인한 이미지 부족")
    void validateAndModifyImages_FailByNoImagesLeft() {
        // given
        Product mockProduct = createMockProduct(1L);
        List<Long> deleteImageIds = List.of(1L);

        ProductImage mockImage = mock(ProductImage.class);
        given(mockImage.getProduct()).willReturn(mockProduct);
        given(productImageRepository.findById(1L)).willReturn(Optional.of(mockImage));
        given(mockProduct.getProductImages()).willReturn(List.of()); // 삭제 후 이미지 없음

        // when & then
        assertThatThrownBy(() -> productImageService.validateAndModifyImages(mockProduct, null, deleteImageIds))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-2")
                .hasFieldOrPropertyWithValue("msg", "이미지는 필수입니다");
    }

    // ======================================= deleteAllProductImages 테스트 ======================================= //

    @Test
    @DisplayName("모든 이미지 삭제 성공")
    void deleteAllProductImages_Success() {
        // given
        Product mockProduct = mock(Product.class);
        ProductImage image1 = mock(ProductImage.class);
        ProductImage image2 = mock(ProductImage.class);
        given(image1.getImageUrl()).willReturn("http://example.com/image1.jpg");
        given(image2.getImageUrl()).willReturn("http://example.com/image2.jpg");
        given(mockProduct.getProductImages()).willReturn(List.of(image1, image2));

        // when
        productImageService.deleteAllProductImages(mockProduct);

        // then
        verify(fileService).deleteFile("http://example.com/image1.jpg");
        verify(fileService).deleteFile("http://example.com/image2.jpg");
    }

    @Test
    @DisplayName("모든 이미지 삭제 성공 - 이미지가 없는 경우")
    void deleteAllProductImages_SuccessNoImages() {
        // given
        Product mockProduct = mock(Product.class);
        given(mockProduct.getProductImages()).willReturn(List.of());

        // when
        productImageService.deleteAllProductImages(mockProduct);

        // then
        verifyNoInteractions(fileService);
    }

    // ======================================= 헬퍼 메서드들 ======================================= //

    private Product createMockProduct(Long id) {
        return mock(Product.class);
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
}