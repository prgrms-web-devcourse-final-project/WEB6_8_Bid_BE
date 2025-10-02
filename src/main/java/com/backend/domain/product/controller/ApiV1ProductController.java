package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.dto.response.MyProductListItemDto;
import com.backend.domain.product.dto.response.ProductListByMemberItemDto;
import com.backend.domain.product.dto.response.ProductListItemDto;
import com.backend.domain.product.dto.response.ProductResponse;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.mapper.ProductMapper;
import com.backend.domain.product.service.ProductService;
import com.backend.global.page.dto.PageDto;
import com.backend.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final ProductService productService;
    private final MemberService memberService;
    private final ProductMapper productMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public RsData<ProductResponse> createProduct(
            @RequestPart("request") @Valid ProductCreateRequest request,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal User user
    ) {
        Member actor = memberService.findMemberByEmail(user.getUsername());
        Product product = productService.createProduct(actor, request, images);

        ProductResponse response = productMapper.toResponse(product);
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
        Page<Product> products = productService.findBySearchPaged(page, size, sort, search);

        PageDto<ProductListItemDto> response = productMapper.toListResponse(products);
        return RsData.ok("상품 목록이 조회되었습니다", response);
    }

    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public RsData<ProductResponse> getProduct(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);

        ProductResponse response = productMapper.toResponse(product);
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
        Member actor = memberService.findMemberByEmail(user.getUsername());
        Product product = productService.getProductById(productId);

        product.checkActorCanModify(actor);

        productService.modifyProduct(product, request, images, deleteImageIds);

        ProductResponse response = productMapper.toResponse(product);
        return RsData.ok("상품이 수정되었습니다", response);
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public RsData<Void> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal User user
    ) {
        Member actor = memberService.findMemberByEmail(user.getUsername());
        Product product = productService.getProductById(productId);

        product.checkActorCanDelete(actor);

        productService.deleteProduct(product);

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
        Member actor = memberService.findMemberByEmail(user.getUsername());
        Page<Product> products = productService.findByMemberPaged(page, size, sort, actor, status);

        PageDto<MyProductListItemDto> response = productMapper.toMyListResponse(products);
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
        Member actor = memberService.findById(memberId).orElseThrow(ProductException::memberNotFound);

        Page<Product> products = productService.findByMemberPaged(page, size, sort, actor, status);

        PageDto<ProductListByMemberItemDto> response = productMapper.toListByMemberResponse(products);
        return RsData.ok("%d번 회원 상품 목록이 조회되었습니다".formatted(memberId), response);
    }
}
