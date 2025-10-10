package com.backend.domain.product.service;

import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.repository.jpa.ProductImageRepository;
import com.backend.global.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * 상품 이미지 처리 서비스
 * - 상품 이미지의 검증, 업로드, 저장, 삭제 처리
 * - 최소 1개, 최대 5개의 이미지 제한
 * - 파일 크기, 확장자 검증
 */
@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final FileService fileService;
    private final ProductImageRepository productImageRepository;

    // ======================================= public methods ======================================= //
    /**
     * 상품 생성 시 이미지 검증 및 생성
     * - 이미지 필수 및 개수 검증 (1~5개)
     * - 파일 검증 (크기, 확장자 등)
     * - 파일 업로드 및 ProductImage 엔티티 생성
     *
     * @param savedProduct 이미지를 추가할 상품
     * @param images 업로드할 이미지 파일 리스트
     * @throws ProductException 이미지가 없거나 검증 실패 시
     */
    public void validateAndCreateImages(Product savedProduct, List<MultipartFile> images) {
        // 이미지 검증
        validateImagesForCreate(images);

        // 이미지 생성
        createProductImages(savedProduct, images);
    }

    /**
     * 상품 수정 시 이미지 검증 및 수정
     * - 기존 이미지 + 새 이미지 개수가 5개 초과 시 예외
     * - 이미지 추가 및 삭제 처리
     * - 모든 이미지 삭제 시 예외 발생 (최소 1개 유지)
     *
     * @param product 수정할 상품
     * @param images 추가할 이미지 파일 리스트 (null 가능)
     * @param deleteImageIds 삭제할 이미지 ID 리스트 (null 가능)
     * @throws ProductException 이미지 검증 실패, 모든 이미지 삭제 시도 등
     */
    public void validateAndModifyImages(Product product, List<MultipartFile> images, List<Long> deleteImageIds) {
        // 이미지 검증
        if (images != null && !images.isEmpty()) {
            validateImagesForModify(product, images);
        }

        // 이미지 생성
        if (images != null && !images.isEmpty()) {
            createProductImages(product, images);
            productImageRepository.flush();
        }

        // 이미지 삭제
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            deleteProductImages(product, deleteImageIds);
        }
    }

    /**
     * 상품 삭제 시 모든 이미지 삭제
     * - 로컬 파일 또는 S3에서 파일 삭제
     * - ProductImage 엔티티는 cascade로 자동 삭제됨
     *
     * @param product 삭제할 상품
     */
    public void deleteAllProductImages(Product product) {
        for (ProductImage pi : product.getProductImages()) {
            fileService.deleteFile(pi.getImageUrl());
        }
    }

    // ======================================= private methods ======================================= //
    /**
     * 여러 이미지 파일 업로드 및 ProductImage 엔티티 생성
     * - 각 파일을 순차적으로 업로드
     * - 업로드 경로: products/{productId}/파일명
     *
     * @param savedProduct 이미지를 추가할 상품
     * @param images 업로드할 이미지 파일 리스트
     */
    private void createProductImages(Product savedProduct, List<MultipartFile> images) {
        for (MultipartFile image : images) {
            String imageUrl = fileService.uploadFile(image, "products/" + savedProduct.getId());
            createProductImage(savedProduct, imageUrl);
        }
    }

    /**
     * 단일 ProductImage 엔티티 생성 및 저장
     * - Product와 양방향 관계 설정
     * - 첫 번째 이미지는 썸네일로 자동 설정
     *
     * @param savedProduct 이미지를 추가할 상품
     * @param imageUrl 업로드된 이미지의 URL
     */
    public void createProductImage(Product savedProduct, String imageUrl) {
        ProductImage productImage = new ProductImage(imageUrl, savedProduct);
        productImageRepository.save(productImage);
        savedProduct.addProductImage(productImage);
    }

    /**
     * 여러 이미지 삭제
     * - 각 이미지의 소유권 검증
     * - 파일 삭제 및 DB에서 제거
     * - 최소 1개 이미지 유지 검증
     *
     * @param product 이미지를 삭제할 상품
     * @param deleteImageIds 삭제할 이미지 ID 리스트
     * @throws ProductException 이미지가 해당 상품에 속하지 않거나 모든 이미지 삭제 시도
     */
    private void deleteProductImages(Product product, List<Long> deleteImageIds) {
        for (Long deleteImageId : deleteImageIds) {
            // 이미지 존재 검증
            ProductImage productImage = productImageRepository.findById(deleteImageId).orElseThrow(ProductException::imageNotFound);
            // 이미지가 해당 상품에 속하는지 검증
            if (!productImage.getProduct().getId().equals(product.getId())) throw ProductException.imageNotBelongToProduct();

            // 파일 삭제
            fileService.deleteFile(productImage.getImageUrl());

            // 상품 이미지 삭제
            product.deleteProductImage(productImage);
            productImageRepository.delete(productImage);
        }

        // 최소 1개 이미지 유지 검증
        if (product.getProductImages().isEmpty()) throw ProductException.imageRequired();
    }

    // ======================================= validation methods ======================================= //
    /**
     * 상품 생성 시 이미지 검증
     * - 이미지 존재 및 개수 (1~5개)
     * - 각 파일의 유효성 검증
     *
     * @param images 검증할 이미지 파일 리스트
     * @throws ProductException 검증 실패 시
     */
    private void validateImagesForCreate(List<MultipartFile> images) {
        // 이미지 존재 검증
        if (images == null || images.isEmpty()) {
            throw ProductException.imageRequired();
        }

        // 이미지 개수 검증
        if (images.size() > 5) {
            throw ProductException.imageMaxCountExceeded();
        }

        // 각 파일 검증 (크기, 확장자 등)
        validateImages(images);
    }

    /**
     * 상품 수정 시 이미지 검증
     * - 기존 이미지 + 새 이미지 개수 검증 (최대 5개)
     * - 각 파일의 유효성 검증
     *
     * @param product 기존 상품
     * @param images 추가할 이미지 파일 리스트
     * @throws ProductException 검증 실패 시
     */
    private void validateImagesForModify(Product product, List<MultipartFile> images) {
        // 이미지 개수 검증
        if (images.size() + product.getProductImages().size() > 5) {
            throw ProductException.imageMaxCountExceeded();
        }

        // 각 파일 검증 (크기, 확장자 등)
        validateImages(images);
    }

    /**
     * 개별 이미지 파일 검증
     * - 파일이 비어있지 않은지 확인
     * - 파일 크기 검증 (최대 5MB)
     * - 파일 확장자 검증 (jpg, jpeg, png, gif, webp만 허용)
     *
     * @param images 검증할 이미지 파일 리스트
     * @throws ProductException 검증 실패 시
     */
    private void validateImages(List<MultipartFile> images) {
        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

        for (MultipartFile image : images) {
            // 빈 파일 검증
            if (image.isEmpty()) {
                throw ProductException.emptyFile();
            }

            // 파일 크기 검증 (5MB 제한)
            if (image.getSize() > 5 * 1024 * 1024) {
                throw ProductException.fileTooLarge();
            }

            // 파일명 유효성 검증
            String filename = image.getOriginalFilename();
            if (filename == null || !filename.contains(".")) {
                throw ProductException.invalidFileName();
            }

            // 파일 확장자 추출 및 검증
            String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            if (!allowedExtensions.contains(extension)) {
                throw ProductException.unsupportedFileType();
            }
        }
    }
}
