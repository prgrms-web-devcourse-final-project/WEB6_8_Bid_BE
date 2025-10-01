package com.backend.domain.product.repository;

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

@RequiredArgsConstructor
public class ProductElasticRepositoryImpl implements ProductElasticRepositoryCustom {
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<ProductDocument> searchProducts(Pageable pageable, ProductSearchDto search) {
        // 검색 쿼리 생성
        Query query = buildSearchQuery(pageable, search);

        // 검색 실행
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
        List<ProductDocument> content = convertToProductDocumentList(searchHits);

        // 총 검색 결과 수
        long totalHits = searchHits.getTotalHits();

        return new PageImpl<>(content, pageable, totalHits);
    }


    private Query buildSearchQuery(Pageable pageable, ProductSearchDto search) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 필터 적용
        applyFilters(boolQuery, search);

        // 정렬 적용
        List<SortOptions> sortOptions = applySorting(pageable.getSort());

        return NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(pageable)
                .withSort(sortOptions)
                .build();
    }

    private void applyFilters(BoolQuery.Builder boolQuery, ProductSearchDto search) {
        // 키워드 (상품명)
        if (search.keyword() != null && !search.keyword().isBlank()) {
            boolQuery.must(m -> m.match(ma -> ma.field("productName").query(search.keyword())));
        }

        // 카테고리
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

        // 지역
        if (search.location() != null && search.location().length > 0) {
            BoolQuery.Builder locationBuilder = new BoolQuery.Builder();

            for (String location : search.location()) {
                locationBuilder.should(s -> s.match(m -> m.field("location").query(location)));
            }
            boolQuery.filter(f -> f.bool(locationBuilder.build()));
        }

        // 배송 가능 여부
        if (search.isDelivery() != null && search.isDelivery()) {
            boolQuery.filter(f -> f.terms(t -> t
                    .field("deliveryMethod")
                    .terms(terms -> terms.value(List.of(
                            FieldValue.of(DeliveryMethod.DELIVERY.name()),
                            FieldValue.of(DeliveryMethod.BOTH.name())
                    )))
            ));
        }

        // 상태
        if (search.status() != null) {
            boolQuery.filter(f -> f.term(t -> t.field("status").value(search.status().getDisplayName())));
        }
    }

    private List<SortOptions> applySorting(Sort sort) {
        List<SortOptions> sortOptions = new ArrayList<>();

        for (Sort.Order order : sort) {
            SortOptions sortOption = SortOptions.of(s -> s
                    .field(f -> f
                            .field(order.getProperty())
                            .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc)
                    )
            );
            sortOptions.add(sortOption);
        }

        // 정렬이 있을 때만 productId 타이브레이커 추가
        if (!sortOptions.isEmpty()) {
            sortOptions.add(SortOptions.of(s -> s
                    .field(f -> f
                            .field("productId")
                            .order(SortOrder.Desc)
                    )
            ));
        }

        return sortOptions;
    }

    private List<ProductDocument> convertToProductDocumentList(SearchHits<ProductDocument> searchHits) {
        return searchHits.stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
