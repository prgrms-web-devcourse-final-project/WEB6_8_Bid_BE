package com.backend.global.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Pageable;

import java.util.function.Function;

public class QueryDslUtil {
    public static <T> void applySorting(
            JPAQuery<T> query,
            Pageable pageable,
            Function<String, Expression<? extends Comparable<?>>> expressionProvider
    ) {
        pageable.getSort().forEach(order -> {
            Expression<? extends Comparable<?>> expression = expressionProvider.apply(order.getProperty());
            if (expression != null) {
                OrderSpecifier<? extends Comparable<?>> orderSpecifier = new OrderSpecifier<>(
                    order.isAscending() ? Order.ASC : Order.DESC,
                    expression
                );
                query.orderBy(orderSpecifier);
            }
        });
    }
}