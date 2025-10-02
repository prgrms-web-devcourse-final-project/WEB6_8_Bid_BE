package com.backend.domain.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import com.backend.domain.member.entity.Member;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.repository.ProductElasticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ProductElasticRepository productElasticRepository;
    private final ElasticsearchClient elasticsearchClient;

    // Elasticsearch를 이용한 검색
    public Page<ProductDocument> searchProducts(
            int page, int size, ProductSearchSortType sort, ProductSearchDto search
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productElasticRepository.searchProducts(pageable, search);
    }

    public Page<ProductDocument> searchProductsByMember(
            int page, int size, ProductSearchSortType sort, Member actor, SaleStatus status
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productElasticRepository.searchProductsByMember(pageable, actor.getId(), status);
    }

    // 문서 저장/업데이트
    public void indexProduct(ProductDocument document) {
        productElasticRepository.save(document);
        log.info("Indexed product: {}", document.getProductId());
    }

    // 문서 삭제
    public void deleteProduct(String id) {
        productElasticRepository.deleteById(id);
        log.info("Deleted product from index: {}", id);
    }

    // 가격만 부분 업데이트 (Script 사용)
    public void updateProductPrice(Long productId, Long newPrice) {
        String id = String.valueOf(productId);

        try {
            elasticsearchClient.update(
                    UpdateRequest.of(u -> u
                            .index("products")
                            .id(id)
                            .doc(Map.of("currentPrice", newPrice))
                    ),
                    ProductDocument.class
            );

            log.info("Updated product {} price to {}", productId, newPrice);
        } catch (IOException e) {
            log.error("Failed to update product {} price", productId, e);
            throw new RuntimeException("Failed to update product price", e);
        }
    }

    // 상태만 부분 업데이트
    public void updateProductStatus(Long productId, String newStatus) {
        String id = String.valueOf(productId);

        try {
            elasticsearchClient.update(
                    UpdateRequest.of(u -> u
                            .index("products")
                            .id(id)
                            .doc(Map.of("status", newStatus))
                    ),
                    ProductDocument.class
            );

            log.info("Updated product {} status to {}", productId, newStatus);
        } catch (IOException e) {
            log.error("Failed to update product {} status", productId, e);
            throw new RuntimeException("Failed to update product status", e);
        }
    }

    // 입찰자 수만 변경 시 - 부분 업데이트
    public void updateProductBidderCount(Long productId, int newBidderCount) {
        String id = String.valueOf(productId);

        try {
            elasticsearchClient.update(
                    UpdateRequest.of(u -> u
                            .index("products")
                            .id(id)
                            .doc(Map.of("bidderCount", newBidderCount))
                    ),
                    ProductDocument.class
            );

            log.info("Updated product {} bidderCount to {}", productId, newBidderCount);
        } catch (IOException e) {
            log.error("Failed to update product {} bidderCount", productId, e);
            throw new RuntimeException("Failed to update product bidderCount", e);
        }
    }

    private Pageable getPageable(int page, int size, ProductSearchSortType sort) {
        page = (page > 0) ? page : 1;
        size = (size > 0 && size <= 100) ? size : 20;
        return PageRequest.of(page - 1, size, sort.toSort());
    }
}
