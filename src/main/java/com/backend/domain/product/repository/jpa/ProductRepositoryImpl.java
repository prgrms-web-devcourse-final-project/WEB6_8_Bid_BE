package com.backend.domain.product.repository.jpa;

import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.Location;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.global.querydsl.QueryDslUtil;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.Arrays;

import static com.backend.domain.product.entity.QProduct.product;

/**
 * Product Custom Repository 구현체
 * - QueryDSL을 사용한 type-safe 동적 쿼리 구현
 * - 복잡한 검색 조건과 정렬을 효율적으로 처리
 */
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Product> findBySearchPaged(Pageable pageable, ProductSearchDto search) {
        BooleanBuilder builder = new BooleanBuilder();

        // 필터 적용
        applyFilters(builder, search);

        return createPagedQuery(builder, pageable);
    }

    @Override
    public Page<Product> findByMemberPaged(Pageable pageable, Long actorId, AuctionStatus status) {
        BooleanBuilder builder = new BooleanBuilder();

        // 필터 적용
        if (actorId != null) builder.and(product.seller.id.eq(actorId));
        if (status != null) builder.and(product.status.eq(status.getDisplayName()));

        return createPagedQuery(builder, pageable);
    }


    /**
     * 페이징 쿼리 생성 공통 로직
     * - 검색 조건 적용
     * - 정렬 적용
     * - 페이징 적용
     * - Count 쿼리 최적화
     *
     * @param builder 검색 조건
     * @param pageable 페이징 정보
     * @return 페이징된 상품 목록
     */
    private Page<Product> createPagedQuery(BooleanBuilder builder, Pageable pageable) {
        // Query 생성
        JPAQuery<Product> productsQuery = createProductsQuery(builder);

        // sort
        applySorting(productsQuery, pageable);

        // paging
        productsQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // total
        JPAQuery<Long> totalQuery = createTotalQuery(builder);

        return PageableExecutionUtils.getPage(productsQuery.fetch(), pageable, totalQuery::fetchOne);
    }

    /**
     * 상품 목록 조회용 검색 필터 적용
     * - null이 아닌 조건만 동적으로 추가
     * - OR 조건과 AND 조건을 조합하여 복잡한 검색 구현
     *
     * @param builder 검색 조건 빌더
     * @param search 검색 DTO
     */
    private void applyFilters(BooleanBuilder builder, ProductSearchDto search) {
        // 키워드 검색 (상품명에 포함)
        if (search.keyword() != null) {
            builder.and(product.productName.containsIgnoreCase(search.keyword()));
        }

        // 카테고리 필터 (복수 선택 가능, OR 조건)
        if (search.category() != null && search.category().length > 0) {
            ProductCategory[] categories = Arrays.stream(search.category())
                    .map(ProductCategory::fromId)
                    .toArray(ProductCategory[]::new);
            builder.and(product.category.in(categories));
        }

        // 지역 필터 (복수 선택 가능, OR 조건)
        if (search.location() != null && search.location().length > 0) {
            BooleanBuilder locationBuilder = new BooleanBuilder();

            for (String location : search.location()) {
                try {
                    Location locationEnum = Location.fromName(location);
                    locationBuilder.or(
                            product.location.contains(locationEnum.getDisplayName())
                            .or(product.location.contains(locationEnum.getShortName()))
                    );
                } catch (Exception e) {
                    locationBuilder.or(product.location.contains(location));
                }
            }

            builder.and(locationBuilder);
        }

        // 배송 가능 여부 필터
        if (search.isDelivery() != null) {
            if (search.isDelivery()) builder.and(product.deliveryMethod.in(DeliveryMethod.DELIVERY, DeliveryMethod.BOTH));
            else builder.and(product.deliveryMethod.in(DeliveryMethod.TRADE));
        }

        // 경매 상태 필터 (BIDDING이 default)
        if (search.status() != null) {
            builder.and(product.status.eq(search.status().getDisplayName()));
        }
    }

    /**
     * 상품 조회 쿼리 생성
     * - 기본 select, from, where 절 구성
     *
     * @param builder 검색 조건
     * @return 상품 조회 쿼리
     */
    private JPAQuery<Product> createProductsQuery(BooleanBuilder builder) {
        return queryFactory
                .selectFrom(product)
                .where(builder);
    }

    /**
     * 정렬 적용
     * - Pageable의 Sort 정보를 QueryDSL OrderSpecifier로 변환
     * - 인기순의 경우 서브쿼리 사용 (입찰자 수 집계)
     *
     * 지원 정렬:
     * - createDate: 최신순 (기본)
     * - currentPrice: 현재가
     * - endTime: 마감임박 순
     * - bidderCount: 인기순 (입찰자 수)
     *
     * @param query 상품 조회 쿼리
     * @param pageable 페이징 정보 (정렬 포함)
     */
    private void applySorting(JPAQuery<Product> query, Pageable pageable) {
        QueryDslUtil.applySorting(query, pageable, property ->
            switch (property) {
                case "createDate" -> product.createDate;
                case "currentPrice" -> product.currentPrice;
                case "endTime" -> product.endTime;
                case "bidderCount" -> product.bidderCount;
                default -> null;
            }
        );
    }

    /**
     * Count 쿼리 생성
     * - 전체 결과 개수 조회 (페이징 정보 계산용)
     * - 정렬 및 페이징 제외로 성능 최적화
     *
     * @param builder 검색 조건
     * @return Count 쿼리
     */
    private JPAQuery<Long> createTotalQuery(BooleanBuilder builder) {
        return queryFactory
                .select(product.count())
                .from(product)
                .where(builder);
    }
}
