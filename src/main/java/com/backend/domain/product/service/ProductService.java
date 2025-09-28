package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.dto.ProductModifyRequest;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.enums.*;
import com.backend.domain.product.repository.ProductImageRepository;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.global.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final FileService fileService;

    // ======================================= create methods ======================================= //
    @Transactional
    public Product create(Member actor, ProductCreateRequest request, List<MultipartFile> images) {
        // 0. 유효성 검증 (location, images)
        validateLocation(request.location(), request.deliveryMethod());
        validateImagesForCreate(images);

        // 1. Product 생성 및 저장
        Product savedProduct = createProduct(actor, request);

        // 2. 이미지 업로드 및 저장
        createProductImages(savedProduct, images);

        return savedProduct;
    }

    public Product createProduct(Member actor, ProductCreateRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                ProductCategory.fromId(request.categoryId()),
                request.initialPrice(),
                request.auctionStartTime(),
                AuctionDuration.fromValue(request.auctionDuration()),
                request.deliveryMethod(),
                request.location(),
                actor
        );
        return productRepository.save(product);
    }

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

    // ======================================= find/get methods ======================================= //
    public Optional<Product> findLatest() {
        return productRepository.findFirstByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Page<Product> findBySearchPaged(
            int page, int size, ProductSearchSortType sort, ProductSearchDto search
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productRepository.findBySearchPaged(pageable, search);
    }

    @Transactional(readOnly = true)
    public Page<Product> findByMemberPaged(
            int page, int size, ProductSearchSortType sort, Member actor, SaleStatus status
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productRepository.findByMemberPaged(pageable, actor.getId(), SaleStatus.fromSaleStatus(status));
    }

    public long count() {
        return productRepository.count();
    }

    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    public Product getProductById(Long productId) {
        return findById(productId).orElseThrow(ProductException::notFound);
    }

    private Pageable getPageable(int page, int size, ProductSearchSortType sort) {
        page = (page > 0) ? page : 1;
        size = (size > 0 && size <= 100) ? size : 20;
        return PageRequest.of(page - 1, size, sort.toSort());
    }

    // ======================================= modify/delete methods ======================================= //
    @Transactional
    public Product modifyProduct(Product product, ProductModifyRequest request, List<MultipartFile> images, List<Long> deleteImageIds) {
        ProductModifyRequest validatedRequest = validateModifyRequest(product, request);
        validateImagesForModify(product, images);

        product.modify(validatedRequest);

        if (images != null && !images.isEmpty()) {
            createProductImages(product, images);
            productImageRepository.flush();
        }

        if (deleteImageIds != null) {
            for (Long deleteImageId : deleteImageIds) {
                ProductImage productImage = productImageRepository.findById(deleteImageId).orElseThrow(ProductException::imageNotFound);
                if (!productImage.getProduct().getId().equals(product.getId())) throw ProductException.imageNotBelongToProduct();

                fileService.deleteFile(productImage.getImageUrl());
                product.deleteProductImage(productImage);
                productImageRepository.delete(productImage);
            }

            if (product.getProductImages().isEmpty()) throw ProductException.imageRequired();
        }

        return product;
    }

    public void deleteProduct(Product product) {
        if (product.getStartTime().isBefore(LocalDateTime.now())) {
            throw ProductException.auctionDeleteForbidden();
        }

        for (ProductImage pi : product.getProductImages()) {
            fileService.deleteFile(pi.getImageUrl());
        }

        productRepository.delete(product);
    }

    // ======================================= validation methods ======================================= //
    private void validateLocation(String location, DeliveryMethod deliveryMethod) {
        if (deliveryMethod == DeliveryMethod.TRADE || deliveryMethod == DeliveryMethod.BOTH) {
            if (location == null || location.isBlank()) {
                throw ProductException.locationRequired();
            }
        }
    }

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

    public ProductModifyRequest validateModifyRequest(Product product, ProductModifyRequest request) {
        if (product.getStartTime().isBefore(LocalDateTime.now())) {
            throw ProductException.auctionModifyForbidden();
        }
        validateLocation(request.location(), request.deliveryMethod());

        String newTitle = request.name();
        String newDescription = request.description();
        Integer newCategoryId = request.categoryId();
        Long newInitialPrice = request.initialPrice();
        LocalDateTime newAuctionStartTime = request.auctionStartTime();
        String newAuctionDuration = request.auctionDuration();
        DeliveryMethod newDeliveryMethod = request.deliveryMethod();
        String newLocation = request.location();

        if (newTitle != null && newTitle.equals(product.getProductName())) newTitle = null;
        if (newDescription != null && newDescription.equals(product.getDescription())) newDescription = null;
        if (newCategoryId != null && ProductCategory.fromId(newCategoryId).equals(product.getCategory())) newCategoryId = null;
        if (newInitialPrice != null && newInitialPrice.equals(product.getInitialPrice())) newInitialPrice = null;
        if (newAuctionStartTime != null && newAuctionStartTime.equals(product.getStartTime())) newAuctionStartTime = null;
        if (newAuctionDuration != null && AuctionDuration.fromValue(newAuctionDuration).equals(product.getDuration())) newAuctionDuration = null;
        if (newDeliveryMethod != null && newDeliveryMethod.equals(product.getDeliveryMethod())) newDeliveryMethod = null;
        if (newLocation != null && newLocation.equals(product.getLocation())) newLocation = null;

        return new ProductModifyRequest(newTitle, newDescription, newCategoryId, newInitialPrice, newAuctionStartTime, newAuctionDuration, newDeliveryMethod, newLocation);
    }
}
