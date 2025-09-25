package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.dto.*;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.service.ProductService;
import com.backend.global.exception.ServiceException;
import com.backend.global.rsData.RsData;
import com.backend.standard.page.dto.PageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApiV1ProductController implements ApiV1ProductControllerDocs {
    private final ProductService productService;
    private final MemberRepository memberRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public RsData<ProductDto> createProduct(
            @RequestPart("request") @Valid ProductCreateRequest request,
            @RequestPart("images") List<MultipartFile> images
//            @AuthenticationPrincipal Member actor
    ) {
        Member actor = memberRepository.findAll().getFirst();

        Product product = productService.create(actor, request, images);

        return new RsData<>("201", "상품이 등록되었습니다.", ProductDto.fromEntity(product));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public RsData<PageDto<ProductListDto>> getProducts(
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

        return new RsData<>(
                "200",
                "상품 목록이 조회되었습니다.",
                PageDto.fromPage(
                        products.map(ProductListDto::fromEntity)
                )
        );
    }

    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public RsData<ProductDto> getProduct(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);

        return new RsData<>(
                "200",
                "상품이 조회되었습니다.",
                ProductDto.fromEntity(product)
        );
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public RsData<ProductDto> modifyProduct(
            @PathVariable Long productId,
            @RequestPart("request") @Valid ProductModifyRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "deleteImageIds", required = false) List<Long> deleteImageIds
//            @AuthenticationPrincipal Member actor
    ) {
        Product product = productService.getProductById(productId);

//        product.checkActorCanModify(actor);

        productService.modifyProduct(product, request, images, deleteImageIds);

        return new RsData<>(
                "200",
                "상품이 수정되었습니다.",
                ProductDto.fromEntity(product)
        );
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public RsData<Void> deleteProduct(
            @PathVariable Long productId
//            @AuthenticationPrincipal Member actor
    ) {
        Product product = productService.getProductById(productId);

//        product.checkActorCanDelete(actor);

        productService.deleteProduct(product);

        return new RsData<>("200", "상품이 삭제되었습니다.", null);
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public RsData<PageDto<MyProductListDto>> getMyProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SELLING") SaleStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
//            @AuthenticationPrincipal Member actor
    ) {
        Member actor = memberRepository.findAll().getFirst();

        Page<Product> products = productService.findByMemberPaged(page, size, sort, actor, status);

        return new RsData<>(
                "200",
                "내 상품 목록이 조회되었습니다.",
                PageDto.fromPage(
                        products.map(MyProductListDto::fromEntity)
                )
        );
    }

    @GetMapping("/members/{memberId}")
    @Transactional(readOnly = true)
    public RsData<PageDto<ProductListByMemberDto>> getProductsByMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SELLING") SaleStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    ) {
        Member actor = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException("404", "존재하지 않는 회원입니다."));

        Page<Product> products = productService.findByMemberPaged(page, size, sort, actor, status);

        return new RsData<>(
                "200",
                "%d번 회원 상품 목록이 조회되었습니다.".formatted(memberId),
                PageDto.fromPage(
                        products.map(ProductListByMemberDto::fromEntity)
                )
        );
    }
}
