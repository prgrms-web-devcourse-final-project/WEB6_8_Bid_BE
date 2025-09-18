package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionDuration;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Product createProduct(Member actor, ProductCreateRequest request, List<MultipartFile> images) {
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
}
