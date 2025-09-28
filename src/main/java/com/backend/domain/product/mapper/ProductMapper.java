package com.backend.domain.product.mapper;

import com.backend.domain.product.dto.response.MyProductListItemDto;
import com.backend.domain.product.dto.response.ProductListByMemberItemDto;
import com.backend.domain.product.dto.response.ProductListItemDto;
import com.backend.domain.product.dto.response.ProductResponse;
import com.backend.domain.product.entity.Product;
import com.backend.global.page.dto.PageDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ProductMapper {
    // ================================
    // Entity → Response 변환
    // ================================
    
    public ProductResponse toResponse(Product product) {
        return ProductResponse.fromEntity(product);
    }
    
    public PageDto<ProductListItemDto> toListResponse(Page<Product> products) {
        return mapToPageDto(products, ProductListItemDto::fromEntity);
    }
    
    public PageDto<MyProductListItemDto> toMyListResponse(Page<Product> product) {
        return mapToPageDto(product, MyProductListItemDto::fromEntity);
    }
    
    public PageDto<ProductListByMemberItemDto> toListByMemberResponse(Page<Product> product) {
        return mapToPageDto(product, ProductListByMemberItemDto::fromEntity);
    }

    // 제네릭 헬퍼 메서드
    private <T> PageDto<T> mapToPageDto(Page<Product> products, Function<Product, T> mapper) {
        return PageDto.fromPage(products.map(mapper));
    }
}