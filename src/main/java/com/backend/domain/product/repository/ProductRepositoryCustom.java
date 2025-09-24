package com.backend.domain.product.repository;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {
    Page<Product> findBySearchPaged(Pageable pageable, ProductSearchDto search);

    Page<Product> findByMemberPaged(Pageable pageable, Member actor, AuctionStatus status);
}
