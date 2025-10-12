package com.backend.domain.product.facade;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.dto.response.*;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.mapper.ProductMapper;
import com.backend.domain.product.service.ProductSearchService;
import com.backend.domain.product.service.ProductService;
import com.backend.domain.product.service.ProductServiceFactory;
import com.backend.global.page.dto.PageDto;
import com.backend.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductFacade {
    private final ProductService productService;
    private final MemberService memberService;
    private final ProductMapper productMapper;
    private final ProductSearchService productSearchService;
    private final ProductServiceFactory productServiceFactory;

    // 상품 생성
    public ProductResponse createProduct(
            String productType,
            ProductCreateRequest request,
            List<MultipartFile> images,
            User user
    ) {
        Member actor = memberService.findMemberByEmail(user.getUsername());

        ProductService service = productServiceFactory.getService(productType);
        Product product = service.createProduct(actor, request, images);

        return productMapper.toResponse(product);
    }

    // 상품 목록 조회
    public PageDto<ProductListItemDto> getProducts(
            int page, int size, ProductSearchSortType sort,
            ProductSearchDto search
    ) {
        Page<Product> products = productService.findBySearchPaged(page, size, sort, search);

        return productMapper.toListResponse(products);
    }

    // 상품 목록 조회 (Elasticsearch)
    public PageDto<ProductListItemDto> getProductsByElasticsearch(
            int page, int size, ProductSearchSortType sort,
            ProductSearchDto search
    ) {
        Page<ProductDocument> products = productSearchService.searchProducts(page, size, sort, search);

        return productMapper.toListResponseFromDocument(products);
    }

    // 상품 상세 조회
    public ProductResponse getProduct(Long productId) {
        Product product = productService.getProductById(productId);

        return productMapper.toResponse(product);
    }

    // 삼품 수정
    public ProductResponse modifyProduct(
            Long productId,
            ProductModifyRequest request,
            List<MultipartFile> images,
            List<Long> deleteImageIds,
            User user
    ) {
        Member actor = memberService.findMemberByEmail(user.getUsername());
        Product product = productService.getProductById(productId);

        product.checkActorCanModify(actor);

        productService.modifyProduct(product, request, images, deleteImageIds);

        return productMapper.toResponse(product);
    }

    // 상품 삭제
    public void deleteProduct(Long productId, User user) {
        Member actor = memberService.findMemberByEmail(user.getUsername());
        Product product = productService.getProductById(productId);

        product.checkActorCanDelete(actor);

        productService.deleteProduct(product);
    }

    // 내 상품 목록 조회
    public PageDto<MyProductListItemDto> getMyProducts(
            int page, int size, ProductSearchSortType sort,
            SaleStatus status, User user
    ) {
        Member actor = memberService.findMemberByEmail(user.getUsername());
        Page<Product> products = productService.findByMemberPaged(page, size, sort, actor, status);

        return productMapper.toMyListResponse(products);
    }

    // 특정 회원 상품 목록 조회
    public PageDto<ProductListByMemberItemDto> getProductsByMember(
            Long memberId,
            int page, int size, ProductSearchSortType sort,
            SaleStatus status
    ) {
        Member actor = memberService.findById(memberId).orElseThrow(ProductException::memberNotFound);

        Page<Product> products = productService.findByMemberPaged(page, size, sort, actor, status);

        return productMapper.toListByMemberResponse(products);
    }

    // 검색어 분석기 reload
    public RsData<ReloadAnalyzersResponse> reloadSearchAnalyzers() {
        return productSearchService.reloadSearchAnalyzers();
    }
}