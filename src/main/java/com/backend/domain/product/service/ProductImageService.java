package com.backend.domain.product.service;

import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.repository.ProductImageRepository;
import com.backend.global.file.service.LocalFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final LocalFileService fileService;
    private final ProductImageRepository productImageRepository;

    // ======================================= public methods ======================================= //
    // 이미지 검증 및 생성 (상품 생성 시 사용)
    public void validateAndCreateImages(Product savedProduct, List<MultipartFile> images) {
        // 이미지 검증
        validateImagesForCreate(images);

        // 이미지 생성
        createProductImages(savedProduct, images);
    }

    // 이미지 검증 및 수정 (상품 수정 시 사용)
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

    // 상품의 모든 이미지 삭제 (상품 삭제 시 사용)
    public void deleteAllProductImages(Product product) {
        for (ProductImage pi : product.getProductImages()) {
            fileService.deleteFile(pi.getImageUrl());
        }
    }

    // ======================================= private methods ======================================= //
    private void createProductImages(Product savedProduct, List<MultipartFile> images) {
        for (MultipartFile image : images) {
            String imageUrl = fileService.uploadFile(image, "products/" + savedProduct.getId());
            createProductImage(savedProduct, imageUrl);
        }
    }

    public void createProductImage(Product savedProduct, String imageUrl) {
        ProductImage productImage = new ProductImage(imageUrl, savedProduct);
        productImageRepository.save(productImage);
        savedProduct.addProductImage(productImage);
    }

    private void deleteProductImages(Product product, List<Long> deleteImageIds) {
        for (Long deleteImageId : deleteImageIds) {
            // 이미지 존재 검증
            ProductImage productImage = productImageRepository.findById(deleteImageId).orElseThrow(ProductException::imageNotFound);
            if (!productImage.getProduct().getId().equals(product.getId())) throw ProductException.imageNotBelongToProduct();

            // 파일 삭제
            fileService.deleteFile(productImage.getImageUrl());

            // 상품 이미지 삭제
            product.deleteProductImage(productImage);
            productImageRepository.delete(productImage);
        }

        if (product.getProductImages().isEmpty()) throw ProductException.imageRequired();
    }

    // ======================================= validation methods ======================================= //
    private void validateImagesForCreate(List<MultipartFile> images) {
        // 이미지 존재 검증
        if (images == null || images.isEmpty()) {
            throw ProductException.imageRequired();
        }

        // 이미지 개수 검증
        if (images.size() > 5) {
            throw ProductException.imageMaxCountExceeded();
        }

        validateImages(images);
    }

    private void validateImagesForModify(Product product, List<MultipartFile> images) {
        // 이미지 개수 검증
        if (images.size() + product.getProductImages().size() > 5) {
            throw ProductException.imageMaxCountExceeded();
        }

        validateImages(images);
    }

    private void validateImages(List<MultipartFile> images) {
        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

        for (MultipartFile image : images) {
            // 이미지 존재 검증
            if (image.isEmpty()) {
                throw ProductException.emptyFile();
            }

            // 파일 크기 검증
            if (image.getSize() > 5 * 1024 * 1024) {
                throw ProductException.fileTooLarge();
            }

            // 파일 확장자 검증
            String filename = image.getOriginalFilename();
            if (filename == null || !filename.contains(".")) {
                throw ProductException.invalidFileName();
            }

            String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            if (!allowedExtensions.contains(extension)) {
                throw ProductException.unsupportedFileType();
            }
        }
    }
}
