package com.backend.domain.product.repository.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Product Elasticsearch Custom Repository 구현체
 * - Elasticsearch Java Client API 사용
 * - Bool Query로 복잡한 검색 조건 조합
 * - Nori Analyzer를 활용한 한글 검색 최적화
 *
 * 쿼리 구조:
 * - must: 필수 조건 (키워드 match)
 * - filter: 필터 조건 (category, location, status 등)
 * - should: 선택적 조건 (향후 확장용)
 */
@RequiredArgsConstructor
public class ProductElasticRepositoryImpl implements ProductElasticRepositoryCustom {
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<ProductDocument> searchProducts(Pageable pageable, ProductSearchDto search) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 필터 적용
        applyFilters(boolQuery, search);

        boolean hasKeyword = search.keyword() != null && !search.keyword().isBlank();
        return createPagedQuery(boolQuery, pageable, hasKeyword);
    }


    /**
     * 페이징 쿼리 생성 및 실행
     * - Bool Query 완성
     * - 정렬 옵션 추가
     * - Elasticsearch 검색 실행
     * - 결과를 Spring Data Page로 변환
     *
     * @param boolQuery 검색 조건
     * @param pageable 페이징 정보
     * @return 페이징된 검색 결과
     */
    private Page<ProductDocument> createPagedQuery(BoolQuery.Builder boolQuery, Pageable pageable, boolean hasKeyword) {
        // 정렬 적용
        List<SortOptions> sortOptions = applySorting(pageable.getSort(), hasKeyword);

        // 검색 쿼리 생성
        Query query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(pageable)
                .withSort(sortOptions)
                .build();

        // 검색 실행
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
        List<ProductDocument> content = convertToProductDocumentList(searchHits);

        // 총 검색 결과 수
        long totalHits = searchHits.getTotalHits();

        return new PageImpl<>(content, pageable, totalHits);
    }

    /**
     * 상품 목록 조회용 검색 필터 적용
     * - must: 키워드 match (형태소 분석)
     * - filter: 정확한 일치 검색 (term, terms)
     *
     * @param boolQuery Bool Query 빌더
     * @param search 검색 조건
     */
    private void applyFilters(BoolQuery.Builder boolQuery, ProductSearchDto search) {
        // 키워드(상품명) 검색 (must - 형태소 분석)
        if (search.keyword() != null && !search.keyword().isBlank()) {
            boolQuery.must(m -> m.match(ma -> ma.field("productName").query(search.keyword())));
        }

        // 카테고리 필터 (filter - 정확한 일치)
        if (search.category() != null && search.category().length > 0) {
            boolQuery.filter(f -> f.terms(t -> t
                    .field("category")
                    .terms(terms -> terms.value(
                            Arrays.stream(search.category())
                                    .map(ProductCategory::fromId)
                                    .map(ProductCategory::name)
                                    .map(FieldValue::of)
                                    .toList()
                    ))
            ));
        }

        // 지역 필터 (filter - match, OR 조건, 형태소 분석)
        if (search.location() != null && search.location().length > 0) {
            BoolQuery.Builder locationBuilder = new BoolQuery.Builder();

            for (String location : search.location()) {
                // should: OR 조건으로 여러 지역 검색
                // match: 형태소 분석으로 유연한 검색
                locationBuilder.should(s -> s.match(m -> m.field("location").query(location)));
            }
            boolQuery.filter(f -> f.bool(locationBuilder.build()));
        }

        // 배송 가능 여부 필터
        if (search.isDelivery() != null && search.isDelivery()) {
            boolQuery.filter(f -> f.terms(t -> t
                    .field("deliveryMethod")
                    .terms(terms -> terms.value(List.of(
                            FieldValue.of(DeliveryMethod.DELIVERY.name()),
                            FieldValue.of(DeliveryMethod.BOTH.name())
                    )))
            ));
        }

        // 경매 상태 필터
        if (search.status() != null) {
            boolQuery.filter(f -> f.term(t -> t.field("status").value(search.status().getDisplayName())));
        }
    }

    /**
     * 정렬 옵션 생성
     * - Spring Data Sort → Elasticsearch SortOptions 변환
     * - 타이브레이커: productId DESC (동일 값 처리)
     *
     * @param sort Spring Data Sort
     * @return Elasticsearch SortOptions 리스트
     */
    private List<SortOptions> applySorting(Sort sort, boolean hasKeyword) {
        List<SortOptions> sortOptions = new ArrayList<>();

        // 1. 사용자가 명시적으로 정렬을 지정한 경우
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                sortOptions.add(SortOptions.of(s -> s
                        .field(f -> f
                                .field(order.getProperty())
                                .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc)
                        )
                ));
            }
        }
        // 2. 정렬 지정 안했지만 키워드 검색이 있는 경우 -> score 정렬
        else if (hasKeyword) {
            sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
        }

        // 타이브레이커 추가 (키워드도 업고, 정렬 기준도 없으면 최신순 정렬)
        sortOptions.add(SortOptions.of(s -> s
                .field(f -> f
                        .field("productId")
                        .order(SortOrder.Desc)
                )
        ));

        return sortOptions;
    }

    /**
     * SearchHits → ProductDocument List 변환
     *
     * @param searchHits Elasticsearch 검색 결과
     * @return ProductDocument 리스트
     */
    private List<ProductDocument> convertToProductDocumentList(SearchHits<ProductDocument> searchHits) {
        return searchHits.stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
