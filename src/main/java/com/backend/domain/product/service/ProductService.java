package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.enums.AuctionDuration;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.ProductImageRepository;
import com.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final FileService fileService;

    public Product createProduct(Member actor, ProductCreateRequest request, List<MultipartFile> images) {
        // 1. Product 생성 및 저장
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
        Product savedProduct = productRepository.save(product);

        // 2. 이미지 업로드 및 저장
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile image : images) {
            String imageUrl = fileService.uploadFile(image, "products/" + savedProduct.getId());
            imageUrls.add(imageUrl);

            ProductImage productImage = new ProductImage(imageUrl, savedProduct);
            productImageRepository.save(productImage);
            savedProduct.addProductImage(productImage);
        }

        return savedProduct;
    }
}
