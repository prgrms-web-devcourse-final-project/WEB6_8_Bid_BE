package com.backend.domain.product.controller;

import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.dto.response.MyProductListItemDto;
import com.backend.domain.product.dto.response.ProductListByMemberItemDto;
import com.backend.domain.product.dto.response.ProductListItemDto;
import com.backend.domain.product.dto.response.ProductResponse;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.facade.ProductFacade;
import com.backend.global.page.dto.PageDto;
import com.backend.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApiV1ProductController implements ApiV1ProductControllerDocs {
    private final ProductFacade productFacade;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public RsData<ProductResponse> createProduct(
            @RequestPart("request") @Valid ProductCreateRequest request,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal User user
    ) {
        ProductResponse response = productFacade.createProduct(request, images, user);
        return RsData.created("상품이 등록되었습니다", response);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public RsData<PageDto<ProductListItemDto>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer[] category,
            @RequestParam(required = false) String[] location,
            @RequestParam(required = false) Boolean isDelivery,
            @RequestParam(defaultValue = "BIDDING") AuctionStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    ) {
        ProductSearchDto search = new ProductSearchDto(keyword, category, location, isDelivery, status);
        PageDto<ProductListItemDto> response = productFacade.getProducts(page, size, sort, search);
        return RsData.ok("상품 목록이 조회되었습니다", response);
    }

    @GetMapping("/es")
    @Transactional(readOnly = true)
    public RsData<PageDto<ProductListItemDto>> getProductsByElasticsearch(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer[] category,
            @RequestParam(required = false) String[] location,
            @RequestParam(required = false) Boolean isDelivery,
            @RequestParam(defaultValue = "BIDDING") AuctionStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    ) {
        ProductSearchDto search = new ProductSearchDto(keyword, category, location, isDelivery, status);
        PageDto<ProductListItemDto> response = productFacade.getProductsByElasticsearch(page, size, sort, search);
        return RsData.ok("상품 목록이 조회되었습니다", response);
    }

    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public RsData<ProductResponse> getProduct(@PathVariable Long productId) {
        ProductResponse response = productFacade.getProduct(productId);
        return RsData.ok("상품이 조회되었습니다", response);
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public RsData<ProductResponse> modifyProduct(
            @PathVariable Long productId,
            @RequestPart("request") @Valid ProductModifyRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @AuthenticationPrincipal User user
    ) {
        ProductResponse response = productFacade.modifyProduct(productId, request, images, deleteImageIds, user);
        return RsData.ok("상품이 수정되었습니다", response);
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public RsData<Void> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal User user
    ) {
        productFacade.deleteProduct(productId, user);
        return RsData.ok("상품이 삭제되었습니다");
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public RsData<PageDto<MyProductListItemDto>> getMyProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SELLING") SaleStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort,
            @AuthenticationPrincipal User user
    ) {
        PageDto<MyProductListItemDto> response = productFacade.getMyProducts(page, size, sort, status, user);
        return RsData.ok("내 상품 목록이 조회되었습니다", response);
    }

    @GetMapping("/members/{memberId}")
    @Transactional(readOnly = true)
    public RsData<PageDto<ProductListByMemberItemDto>> getProductsByMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SELLING") SaleStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    ) {
        PageDto<ProductListByMemberItemDto> response = productFacade.getProductsByMember(memberId, page, size, sort, status);
        return RsData.ok("%d번 회원 상품 목록이 조회되었습니다".formatted(memberId), response);
    }
}
