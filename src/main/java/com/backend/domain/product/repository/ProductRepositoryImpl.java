package com.backend.domain.product.repository;

import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.Location;
import com.backend.domain.product.enums.ProductCategory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;

import static com.backend.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Product> findBySearchPaged(Pageable pageable, ProductSearchDto search) {
        BooleanBuilder builder = new BooleanBuilder();

        // 필터 적용
        applyFilters(builder, search);
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
            builder.and(product.deliveryMethod.eq(DeliveryMethod.DELIVERY));
        }
    }
}
