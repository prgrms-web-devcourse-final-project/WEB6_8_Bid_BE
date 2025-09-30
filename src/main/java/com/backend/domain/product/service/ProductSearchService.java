package com.backend.domain.product.service;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.repository.ProductElasticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ProductElasticRepository productElasticRepository;

    public Page<ProductDocument> searchProducts(
            int page,
            int size,
            ProductSearchSortType sort,
            ProductSearchDto search
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productElasticRepository.searchProducts(pageable, search);
    }

    private Pageable getPageable(int page, int size, ProductSearchSortType sort) {
        page = (page > 0) ? page : 1;
        size = (size > 0 && size <= 100) ? size : 20;
        return PageRequest.of(page - 1, size, sort.toSort());
    }
}
