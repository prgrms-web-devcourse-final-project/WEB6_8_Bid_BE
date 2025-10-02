package com.backend.domain.product.repository;

import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.Location;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.global.querydsl.QueryDslUtil;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.Arrays;

import static com.backend.domain.bid.entity.QBid.bid;
import static com.backend.domain.product.entity.QProduct.product;

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

    private void applyFilters(BooleanBuilder builder, ProductSearchDto search) {
        if (search.keyword() != null) {
            builder.and(product.productName.containsIgnoreCase(search.keyword()));
        }
        if (search.category() != null && search.category().length > 0) {
            ProductCategory[] categories = Arrays.stream(search.category())
                    .map(ProductCategory::fromId)
                    .toArray(ProductCategory[]::new);
            builder.and(product.category.in(categories));
        }
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
        if (search.isDelivery() != null && search.isDelivery()) {
            builder.and(product.deliveryMethod.in(DeliveryMethod.DELIVERY, DeliveryMethod.BOTH));
        }
        if (search.status() != null) {
            builder.and(product.status.eq(search.status().getDisplayName()));
        }
    }

    private JPAQuery<Product> createProductsQuery(BooleanBuilder builder) {
        return queryFactory
                .selectFrom(product)
                .where(builder);
    }

    private void applySorting(JPAQuery<Product> query, Pageable pageable) {
        QueryDslUtil.applySorting(query, pageable, property ->
            switch (property) {
                case "createDate" -> product.createDate;
                case "currentPrice" -> product.currentPrice;
                case "endTime" -> product.endTime;
                case "bidderCount" -> JPAExpressions
                        .select(bid.member.id.countDistinct())
                        .from(bid)
                        .where(bid.product.eq(product));
                default -> null;
            }
        );
    }

    private JPAQuery<Long> createTotalQuery(BooleanBuilder builder) {
        return queryFactory
                .select(product.count())
                .from(product)
                .where(builder);
    }
}
