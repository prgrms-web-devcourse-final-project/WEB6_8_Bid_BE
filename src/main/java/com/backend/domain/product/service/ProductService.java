package com.backend.domain.product.service;

import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Product createProduct(ProductCreateRequest request, List<MultipartFile> images) {
        return new Product();
    }
}
