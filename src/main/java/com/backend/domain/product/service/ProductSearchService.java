package com.backend.domain.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.repository.elasticsearch.ProductElasticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Elasticsearch 기반 상품 검색 서비스
 * - 전문 검색 기능 제공 (한글 형태소 분석)
 * - 복잡한 필터링 및 정렬 지원
 * - 부분 업데이트를 통한 효율적인 인덱스 관리
 *
 * 주요 기능:
 * - 상품 검색 (키워드, 카테고리, 지역 등)
 * - 회원별 상품 검색
 * - 문서 CRUD (인덱싱, 업데이트, 삭제)
 * - 부분 업데이트 (가격, 상태, 입찰자 수)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ProductElasticRepository productElasticRepository;
    private final ElasticsearchClient elasticsearchClient;

    // ======================================= search methods ======================================= //
    /**
     * 검색 조건에 따른 상품 검색
     * - 키워드(상품명), 카테고리, 지역, 배송 가능 여부, 경매 상태로 필터링
     * - 상품명, 지역에 대한 전문 검색 (nori analyzer 사용)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 기준 (LATEST, PRICE_HIGH, PRICE_LOW, ENDING_SOON, POPULAR)
     * @param search 검색 조건 (keyword, category, location, isDelivery, status)
     * @return 페이징된 ProductDocument 목록
     */
    public Page<ProductDocument> searchProducts(
            int page, int size, ProductSearchSortType sort, ProductSearchDto search
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productElasticRepository.searchProducts(pageable, search);
    }

    /**
     * Pageable 객체 생성 및 검증
     * - page: 1 이상 (기본값: 1)
     * - size: 1~100 (기본값: 20)
     * - sort: ProductSearchSortType을 Spring Data Sort로 변환
     */
    private Pageable getPageable(int page, int size, ProductSearchSortType sort) {
        page = (page > 0) ? page : 1;
        size = (size > 0 && size <= 100) ? size : 20;

        if (sort == null) {
            return PageRequest.of(page, size, Sort.unsorted());
        }

        return PageRequest.of(page - 1, size, sort.toSort());
    }

    // ======================================= document methods ======================================= //
    /**
     * 상품 문서 인덱싱 (생성/전체 업데이트)
     * - 새 상품 등록 시 호출
     * - 상품 정보 수정 시 호출 (여러 필드 변경)
     * - 전체 재인덱싱 시 호출
     *
     * @param document 인덱싱할 ProductDocument
     */
    public void indexProduct(ProductDocument document) {
        productElasticRepository.save(document);
        log.info("Indexed product: {}", document.getProductId());
    }

    /**
     * 상품 문서 삭제
     * - 상품 삭제 시 호출
     * - Elasticsearch 인덱스에서 해당 문서 제거
     *
     * @param id Elasticsearch 문서 ID (Product ID의 문자열 형태)
     */
    public void deleteProduct(String id) {
        productElasticRepository.deleteById(id);
        log.info("Deleted product from index: {}", id);
    }

    /**
     * 가격 부분 업데이트
     * - 입찰로 인한 가격 변경 시 사용
     * - currentPrice 필드만 업데이트 (전체 재인덱싱 불필요)
     * - Elasticsearch Update API 사용
     *
     * @param productId 상품 ID
     * @param newPrice 변경된 가격
     * @throws RuntimeException Elasticsearch 업데이트 실패 시
     */
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

    /**
     * 상태 부분 업데이트
     * - 경매 상태 변경 시 사용 (BEFORE_START -> BIDDING -> SUCCESSFUL/FAILED)
     * - status 필드만 업데이트 (전체 재인덱싱 불필요)
     * - 스케줄러에 의한 자동 상태 변경 시 호출
     *
     * @param productId 상품 ID
     * @param newStatus 변경된 상태 (AuctionStatus의 displayName)
     */
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

    /**
     * 입찰자 수 부분 업데이트
     * - 새로운 입찰자가 입찰 시 사용
     * - bidderCount 필드만 업데이트 (전체 재인덱싱 불필요)
     * - 인기순 정렬에 영향을 미치는 중요 필드
     *
     * @param productId 상품 ID
     * @param newBidderCount 변경된 입찰자 수
     */
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
}
