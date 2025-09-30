package com.backend.domain.product.repository;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductElasticRepositoryCustom {
    Page<ProductDocument> searchProducts(Pageable pageable, ProductSearchDto search);
}
