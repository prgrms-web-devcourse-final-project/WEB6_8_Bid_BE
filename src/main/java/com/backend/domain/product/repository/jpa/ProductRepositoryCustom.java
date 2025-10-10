package com.backend.domain.product.repository.jpa;

import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {
    Page<Product> findBySearchPaged(Pageable pageable, ProductSearchDto search);

    Page<Product> findByMemberPaged(Pageable pageable, Long actorId, AuctionStatus status);
}
