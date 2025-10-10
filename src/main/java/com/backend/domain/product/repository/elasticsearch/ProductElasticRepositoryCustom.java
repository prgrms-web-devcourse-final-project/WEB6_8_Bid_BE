package com.backend.domain.product.repository.elasticsearch;

import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductElasticRepositoryCustom {
    Page<ProductDocument> searchProducts(Pageable pageable, ProductSearchDto search);

    Page<ProductDocument> searchProductsByMember(Pageable pageable, Long actorId, SaleStatus status);
}
