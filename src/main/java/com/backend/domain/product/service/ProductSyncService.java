package com.backend.domain.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * RDB와 Elasticsearch 간 데이터 동기화 서비스
 * - 상품 생성/수정/삭제 시 Elasticsearch 인덱스 자동 업데이트
 * - 부분 업데이트를 통한 효율적인 동기화 (가격, 상태, 입찰자 수)
 * - 전체 재인덱싱 기능 제공 (초기 설정 또는 복구용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {
    private final ProductRepository productRepository;
    private final ProductSearchService productSearchService;
    private final ElasticsearchClient elasticsearchClient;

    private static final String ALIAS = "products";
    private static final String INITIAL_INDEX = "products_v1";

    /**
     * 상품 생성 시 Elasticsearch 동기화
     * - Product 엔티티를 ProductDocument로 변환
     * - Elasticsearch에 새 문서 인덱싱
     *
     * @param product 생성된 상품 엔티티
     */
    public void syncProductCreation(Product product) {
        try {
            ProductDocument document = ProductDocument.fromEntity(product);
            productSearchService.indexProduct(document);
        } catch (Exception e) {
            log.error("Failed to sync product creation to Elasticsearch: {}", product.getId(), e);
            // 동기화 실패 시에도 트랜잭션은 정상 진행 (Elasticsearch는 보조 저장소)
        }
    }

    /**
     * 상품 수정 시 Elasticsearch 동기화 (전체 업데이트)
     * - 모든 필드가 변경될 수 있으므로 전체 문서 업데이트
     * - Product 엔티티를 ProductDocument로 변환 후 재인덱싱
     *
     * @param product 수정된 상품 엔티티
     */
    public void syncProductUpdate(Product product) {
        try {
            ProductDocument document = ProductDocument.fromEntity(product);
            productSearchService.indexProduct(document);
        } catch (Exception e) {
            log.error("Failed to sync product update to Elasticsearch: {}", product.getId(), e);
        }
    }

    /**
     * 상품 삭제 시 Elasticsearch 동기화
     * - Elasticsearch 인덱스에서 해당 문서 삭제
     *
     * @param productId 삭제된 상품 ID
     */
    public void syncProductDeletion(Long productId) {
        try {
            productSearchService.deleteProduct(String.valueOf(productId));
        } catch (Exception e) {
            log.error("Failed to sync product deletion to Elasticsearch: {}", productId, e);
        }
    }

    /**
     * 가격 변경 시 부분 업데이트
     * - 입찰로 인한 가격 변경 시 호출
     * - 전체 문서를 재인덱싱하지 않고 currentPrice 필드만 업데이트
     * - 이벤트 기반으로 비동기 처리됨 (ProductEventListener에서 호출)
     *
     * @param productId 상품 ID
     * @param newPrice 변경된 가격
     */
    public void syncProductPriceUpdate(Long productId, Long newPrice) {
        try {
            productSearchService.updateProductPrice(productId, newPrice);
        } catch (Exception e) {
            log.error("Failed to sync product price update to Elasticsearch: {}", productId, e);
        }
    }

    /**
     * 상태 변경 시 부분 업데이트
     * - 경매 상태 변경 시 호출 (경매 시작 전 -> 경매 중 -> 낙찰/유찰)
     * - 전체 문서를 재인덱싱하지 않고 status 필드만 업데이트
     * - 이벤트 기반으로 비동기 처리됨 (ProductEventListener에서 호출)
     *
     * @param productId 상품 ID
     * @param newStatus 변경된 상태
     */
    public void syncProductStatusUpdate(Long productId, String newStatus) {
        try {
            productSearchService.updateProductStatus(productId, newStatus);
        } catch (Exception e) {
            log.error("Failed to sync product status update to Elasticsearch: {}", productId, e);
        }
    }

    /**
     * 입찰자 수 변경 시 부분 업데이트
     * - 새로운 입찰자가 입찰 시 호출
     * - 전체 문서를 재인덱싱하지 않고 bidderCount 필드만 업데이트
     * - 이벤트 기반으로 비동기 처리됨 (ProductEventListener에서 호출)
     *
     * @param productId 상품 ID
     * @param newBidderCount 변경된 입찰자 수
     */
    public void syncProductBidderCountUpdate(Long productId, int newBidderCount) {
        try {
            productSearchService.updateProductBidderCount(productId, newBidderCount);
        } catch (Exception e) {
            log.error("Failed to sync product bidder count update to Elasticsearch: {}", productId, e);
        }
    }
    
    /**
     * 전체 상품 재인덱싱
     * - RDB의 모든 상품을 Elasticsearch에 재인덱싱
     * - 초기 설정, 데이터 불일치 복구, 인덱스 재생성, 마이그레이션 시 사용
     * - 배치 단위(100개)로 처리하여 메모리 효율성 확보
     */
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

            // 각 상품을 Elasticsearch에 인덱싱
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

            // 진행률 로깅 (10% 단위 또는 마지막 페이지)
            if ((page + 1) % 10 == 0 || page == totalPages - 1) {
                int progress = (page + 1) * 100 / totalPages;
                log.info("진행: {}% ({}/{} 페이지, 성공: {}, 실패: {})",
                        progress, page + 1, totalPages, successCount, failCount);
            }
        }

        log.info("========== 재인덱싱 완료 ==========");
        log.info("성공: {}, 실패: {}, 총: {}", successCount, failCount, totalCount);

        try {
            createAlias();
        } catch (IOException e) {
            log.error("Alias 실패", e);
        }
    }

    private void createAlias() throws IOException {
        boolean aliasExists = elasticsearchClient.indices()
                .existsAlias(a -> a.name(ALIAS))
                .value();
        if (aliasExists) {
            log.info("Alias '{}' 이미 존재함", ALIAS);
            return;
        }

        log.info("'{}' 인덱스를 '{}' 로 복사 중...", ALIAS, INITIAL_INDEX);
        elasticsearchClient.reindex(r -> r
                .source(s -> s.index(ALIAS))
                .dest(d -> d.index(INITIAL_INDEX))
                .waitForCompletion(true)
        );

        log.info("복사 완료. 구 인덱스 삭제 중...");
        elasticsearchClient.indices().delete(d -> d.index(ALIAS));

        log.info("Alias 생성: {} → {}", ALIAS, INITIAL_INDEX);
        elasticsearchClient.indices().updateAliases(u -> u
                .actions(a -> a
                        .add(add -> add.index(INITIAL_INDEX).alias(ALIAS))
                )
        );

        log.info("Alias 설정 완료!");
    }
}