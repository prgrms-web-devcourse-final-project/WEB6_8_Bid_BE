package com.backend.domain.product.mapper;

import com.backend.domain.member.service.MemberService;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.response.MyProductListItemDto;
import com.backend.domain.product.dto.response.ProductListByMemberItemDto;
import com.backend.domain.product.dto.response.ProductListItemDto;
import com.backend.domain.product.dto.response.ProductResponse;
import com.backend.domain.product.dto.response.component.BidderDto;
import com.backend.domain.product.dto.response.component.ReviewDto;
import com.backend.domain.product.dto.response.component.SellerDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.service.ProductService;
import com.backend.global.page.dto.PageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Product Entity/Document → DTO 변환 매퍼
 * - Entity와 DTO 간 변환 로직을 중앙 집중화
 * - RDB 조회 결과와 Elasticsearch 조회 결과를 동일한 DTO로 변환
 * - 페이징 처리 및 연관 엔티티 조회 포함
 */
@Component
@RequiredArgsConstructor
public class ProductMapper {
    private final MemberService memberService;
    private final ProductService productService;

    // ======================================= Entity → Response 변환 ======================================= //
    // Product -> ProductResponse (상품 생성, 상세 조회, 수정)
    public ProductResponse toResponse(Product product) {
        return ProductResponse.fromEntity(product);
    }

    // Page<Product> -> PageDto<ProductListItemDto> (상품 목록 조회)
    public PageDto<ProductListItemDto> toListResponse(Page<Product> products) {
        return mapToPageDto(products, ProductListItemDto::fromEntity);
    }

    // Page<Product> -> PageDto<MyProductListItemDto> (내 상품 목록 조회)
    public PageDto<MyProductListItemDto> toMyListResponse(Page<Product> products) {
        return mapToPageDto(products, MyProductListItemDto::fromEntity);
    }

    // Page<Product> -> PageDto<ProductListByMemberItemDto> (특정 회원의 상품 목록 조회)
    public PageDto<ProductListByMemberItemDto> toListByMemberResponse(Page<Product> products) {
        return mapToPageDto(products, ProductListByMemberItemDto::fromEntity);
    }

    // Page<ProductDocument> -> PageDto<ProductListItemDto> (상품 목록 조회 - ElasticSearch)
    public PageDto<ProductListItemDto> toListResponseFromDocument(Page<ProductDocument> products) {
        return mapToPageDtoFromDocuments(products, doc -> ProductListItemDto.fromDocument(doc, getSellerDto(doc)));
    }

    // Page<ProductDocument> -> PageDto<MyProductListItemDto> (내 상품 목록 조회 - ElasticSearch)
    public PageDto<MyProductListItemDto> toMyListResponseFromDocument(Page<ProductDocument> products) {
        return mapToPageDtoFromDocuments(products, doc -> MyProductListItemDto.fromDocument(doc, getBidderDto(doc), getReviewDto(doc)));
    }

    // Page<ProductDocument> -> PageDto<ProductListByMemberItemDto> (특정 회원의 상품 목록 조회 - ElasticSearch)
    public PageDto<ProductListByMemberItemDto> toListByMemberResponseFromDocument(Page<ProductDocument> products) {
        return mapToPageDtoFromDocuments(products, doc -> ProductListByMemberItemDto.fromDocument(doc, getReviewDto(doc)));
    }


    // ======================================= 헬퍼 메서드 ======================================= //
    // Page<Product> -> PageDto<T>
    private <T> PageDto<T> mapToPageDto(Page<Product> products, Function<Product, T> mapper) {
        return PageDto.fromPage(products.map(mapper));
    }

    // Page<ProductDocument> -> PageDto<T>
    private <T> PageDto<T> mapToPageDtoFromDocuments(Page<ProductDocument> documents, Function<ProductDocument, T> mapper) {
        return PageDto.fromPage(documents.map(mapper));
    }

    // ProductDocument -> SellerDto (상품 목록 조회 - ElasticSearch )
    private SellerDto getSellerDto(ProductDocument document) {
        return memberService.findById(document.getSellerId())
                .map(SellerDto::fromEntity)
                .orElse(null);
    }

    // ProductDocument -> BidderDto (내 상품 목록 조회 - Elasticsearch)
    private BidderDto getBidderDto(ProductDocument document) {
        Product product = productService.findById(document.getProductId()).get();
        return BidderDto.fromEntity(product.getBidder());
    }

    // ProductDocument -> ReviewDto (내 상품 목록 조회, 특정 회원의 상품 목록 조회 - Elasticsearch)
    private ReviewDto getReviewDto(ProductDocument document) {
        Product product = productService.findById(document.getProductId()).get();
        return ReviewDto.fromEntity(product.getReview());
    }
}