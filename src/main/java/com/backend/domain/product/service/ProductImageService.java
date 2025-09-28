package com.backend.domain.product.service;

import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final FileService fileService;
    private final ProductImageRepository productImageRepository;

    // ======================================= public methods ======================================= //
    public void validateAndCreateImages(Product savedProduct, List<MultipartFile> images) {
        validateImagesForCreate(images);
        createProductImages(savedProduct, images);
    }

    public void validateAndModifyImages(Product product, List<MultipartFile> images, List<Long> deleteImageIds) {
        validateImagesForModify(product, images);

        if (images != null && !images.isEmpty()) {
            createProductImages(product, images);
            productImageRepository.flush();
        }

        if (deleteImageIds != null) {
            deleteProductImages(product, deleteImageIds);
        }
    }

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
            ProductImage productImage = productImageRepository.findById(deleteImageId).orElseThrow(ProductException::imageNotFound);
            if (!productImage.getProduct().getId().equals(product.getId())) throw ProductException.imageNotBelongToProduct();

            fileService.deleteFile(productImage.getImageUrl());
            product.deleteProductImage(productImage);
            productImageRepository.delete(productImage);
        }

        if (product.getProductImages().isEmpty()) throw ProductException.imageRequired();
    }

    // ======================================= validation methods ======================================= //
    private void validateImagesForCreate(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw ProductException.imageRequired();
        }

        if (images.size() > 5) {
            throw ProductException.imageMaxCountExceeded();
        }

        validateImages(images);
    }

    private void validateImagesForModify(Product product, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        if (images.size() + product.getProductImages().size() > 5) {
            throw ProductException.imageMaxCountExceeded();
        }

        validateImages(images);
    }

    private void validateImages(List<MultipartFile> images) {
        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

        for (MultipartFile image : images) {
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
