package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.*;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.repository.ProductRepository;
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

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductImageService productImageService;
    private final ProductRepository productRepository;
    private final ProductSyncService productSyncService;

    // ======================================= create methods ======================================= //
    @Transactional
    public Product createProduct(Member actor, ProductCreateRequest request, List<MultipartFile> images) {
        // location 검증
        validateLocation(request.location(), request.deliveryMethod());

        // Product 생성 및 저장
        Product savedProduct = saveProduct(actor, request);

        // 이미지 검증 및 저장
        productImageService.validateAndCreateImages(savedProduct, images);

        // Elasticsearch 동기화
        productSyncService.syncProductCreation(savedProduct);

        return savedProduct;
    }

    public Product saveProduct(Member actor, ProductCreateRequest request) {
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

    // ======================================= find/get methods ======================================= //
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

    private Pageable getPageable(int page, int size, ProductSearchSortType sort) {
        page = (page > 0) ? page : 1;
        size = (size > 0 && size <= 100) ? size : 20;
        return PageRequest.of(page - 1, size, sort.toSort());
    }

    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    public Product getProductById(Long productId) {
        return findById(productId).orElseThrow(ProductException::notFound);
    }

    public Optional<Product> findLatest() {
        return productRepository.findFirstByOrderByIdDesc();
    }

    public long count() {
        return productRepository.count();
    }

    // ======================================= modify/delete methods ======================================= //
    @Transactional
    public Product modifyProduct(Product product, ProductModifyRequest request, List<MultipartFile> images, List<Long> deleteImageIds) {
        // 수정 요청 검증
        ProductModifyRequest validatedRequest = validateModifyRequest(product, request);

        // Product 수정
        product.modify(validatedRequest);

        // 이미지 검증 및 수정
        productImageService.validateAndModifyImages(product, images, deleteImageIds);

        // Elasticsearch 동기화
        productSyncService.syncProductUpdate(product);

        return product;
    }

    @Transactional
    public void deleteProduct(Product product) {
        // 삭제 권한 검증
        if (product.getStartTime().isBefore(LocalDateTime.now())) {
            throw ProductException.auctionDeleteForbidden();
        }

        // 이미지 및 Product 삭제
        productImageService.deleteAllProductImages(product);
        productRepository.delete(product);

        // Elasticsearch 동기화
        productSyncService.syncProductDeletion(product.getId());
    }

    // ======================================= validation methods ======================================= //
    private void validateLocation(String location, DeliveryMethod deliveryMethod) {
        if (deliveryMethod == DeliveryMethod.TRADE || deliveryMethod == DeliveryMethod.BOTH) {
            if (location == null || location.isBlank()) {
                throw ProductException.locationRequired();
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
