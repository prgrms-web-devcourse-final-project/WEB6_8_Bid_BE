package com.backend.domain.product.service;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 가격만 변경 시 - 부분 업데이트
    public void syncProductPriceUpdate(Long productId, Long newPrice) {
        try {
            productSearchService.updateProductPrice(productId, newPrice);
        } catch (Exception e) {
            log.error("Failed to sync product price update to Elasticsearch: {}", productId, e);
        }
    }

    // 상태만 변경 시 - 부분 업데이트
    public void syncProductStatusUpdate(Long productId, String newStatus) {
        try {
            productSearchService.updateProductStatus(productId, newStatus);
        } catch (Exception e) {
            log.error("Failed to sync product status update to Elasticsearch: {}", productId, e);
        }
    }

    // 입찰자 수만 변경 시 - 부분 업데이트
    public void syncProductBidderCountUpdate(Long productId, int newBidderCount) {
        try {
            productSearchService.updateProductBidderCount(productId, newBidderCount);
        } catch (Exception e) {
            log.error("Failed to sync product bidder count update to Elasticsearch: {}", productId, e);
        }
    }
    
    // 전체 상품 재인덱싱 (초기 설정 또는 복구용)
    @Transactional(readOnly = true)
    public void reindexAllProducts() {
        log.info("========== 전체 상품 재인덱싱 시작 ==========");

        long totalCount = productRepository.count();
        log.info("총 {}개 상품 인덱싱", totalCount);

        if (totalCount == 0) {
            log.warn("인덱싱할 상품이 없습니다");
            return;
        }

        int batchSize = 100;
        int totalPages = (int) Math.ceil((double) totalCount / batchSize);
        int successCount = 0;
        int failCount = 0;

        for (int page = 0; page < totalPages; page++) {
            Page<Product> productPage = productRepository.findAll(PageRequest.of(page, batchSize));

            for (Product product : productPage.getContent()) {
                try {
                    ProductDocument document = ProductDocument.fromEntity(product);
                    productSearchService.indexProduct(document);
                    successCount++;
                } catch (Exception e) {
                    log.error("상품 {} 인덱싱 실패", product.getId(), e);
                    failCount++;
                }
            }

            if ((page + 1) % 10 == 0 || page == totalPages - 1) {
                int progress = (page + 1) * 100 / totalPages;
                log.info("진행: {}% ({}/{} 페이지, 성공: {}, 실패: {})",
                        progress, page + 1, totalPages, successCount, failCount);
            }
        }

        log.info("========== 재인덱싱 완료 ==========");
        log.info("성공: {}, 실패: {}, 총: {}", successCount, failCount, totalCount);
    }
}