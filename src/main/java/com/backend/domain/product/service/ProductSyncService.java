package com.backend.domain.product.service;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {
    
    private final ProductRepository productRepository;
    private final ProductSearchService productSearchService;
    
    // 상품 생성 시 Elasticsearch 동기화
    public void syncProductCreation(Product product) {
        try {
            ProductDocument document = ProductDocument.fromEntity(product);
            productSearchService.indexProduct(document);
        } catch (Exception e) {
            log.error("Failed to sync product creation to Elasticsearch: {}", product.getId(), e);
        }
    }
    
    // 상품 수정 시 Elasticsearch 동기화
    public void syncProductUpdate(Product product) {
        try {
            ProductDocument document = ProductDocument.fromEntity(product);
            productSearchService.indexProduct(document);
        } catch (Exception e) {
            log.error("Failed to sync product update to Elasticsearch: {}", product.getId(), e);
        }
    }
    
    // 상품 삭제 시 Elasticsearch 동기화
    public void syncProductDeletion(Long productId) {
        try {
            productSearchService.deleteProduct(String.valueOf(productId));
        } catch (Exception e) {
            log.error("Failed to sync product deletion to Elasticsearch: {}", productId, e);
        }
    }
    
    // 전체 상품 재인덱싱 (초기 설정 또는 복구용)
    @Transactional(readOnly = true)
    public void reindexAllProducts() {
        log.info("Starting full reindex of all products");
        List<Product> products = productRepository.findAll();
        
        for (Product product : products) {
            try {
                ProductDocument document = ProductDocument.fromEntity(product);
                productSearchService.indexProduct(document);
            } catch (Exception e) {
                log.error("Failed to reindex product: {}", product.getId(), e);
            }
        }
        
        log.info("Completed reindexing {} products", products.size());
    }
}