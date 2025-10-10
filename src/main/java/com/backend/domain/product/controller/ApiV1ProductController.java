package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.dto.response.*;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.mapper.ProductMapper;
import com.backend.domain.product.service.ProductSearchService;
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

/**
 * 상품 관련 REST API 컨트롤러
 * - 경매 상품의 CRUD 작업을 처리
 * - RDB와 Elasticsearch 기반 검색 기능 제공
 * - 멀티파트 파일 업로드를 통한 이미지 처리
 */
@RestController
@RequiredArgsConstructor
public class ApiV1ProductController implements ApiV1ProductControllerDocs {
    private final ProductService productService;
    private final MemberService memberService;
    private final ProductMapper productMapper;
    private final ProductSearchService productSearchService;

    /**
     * 상품 등록
     * - 상품 정보와 이미지를 함께 업로드하여 새 경매 상품 생성
     * - 이미지는 최소 1개, 최대 5개까지 업로드 가능
     * - 상품 생성 시 Elasticsearch에도 자동으로 인덱싱됨
     *
     * @param request 상품 등록 요청 정보 (JSON)
     * @param images 상품 이미지 파일 리스트 (최소 1개, 최대 5개)
     * @param user 현재 로그인한 사용자 정보
     * @return 생성된 상품의 상세 정보
     */
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

    /**
     * 상품 목록 조회 (RDB 기반)
     * - 다양한 필터 조건으로 상품 검색
     * - QueryDSL을 사용한 동적 쿼리 생성
     *
     * @param page 페이지 번호 (1부터 시작, 기본값: 1)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @param keyword 상품명 검색 키워드
     * @param category 카테고리 ID 배열 (복수 선택 가능)
     * @param location 거래 지역 배열 (복수 선택 가능)
     * @param isDelivery 택배 가능 여부 필터
     * @param status 경매 상태 (BIDDING, BEFORE_START 등)
     * @param sort 정렬 기준 (LATEST, PRICE_HIGH, PRICE_LOW, ENDING_SOON, POPULAR)
     * @return 페이징된 상품 목록
     */
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

    /**
     * 상품 목록 조회 (Elasticsearch 기반)
     * - Elasticsearch의 전문 검색 기능 활용
     * - 한글 형태소 분석(nori analyzer) 지원
     * - RDB보다 빠른 검색 성능 제공
     */
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
            @RequestParam(required = false) ProductSearchSortType sort
    ) {
        ProductSearchDto search = new ProductSearchDto(keyword, category, location, isDelivery, status);
        Page<ProductDocument> products = productSearchService.searchProducts(page, size, sort, search);

        PageDto<ProductListItemDto> response = productMapper.toListResponseFromDocument(products);
        return RsData.ok("상품 목록이 조회되었습니다", response);
    }

    /**
     * 상품 상세 조회
     * - 특정 상품의 모든 정보를 조회
     * - 이미지 목록, 판매자 정보 포함
     *
     * @param productId 조회할 상품의 ID
     * @return 상품 상세 정보
     */
    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public RsData<ProductResponse> getProduct(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);

        ProductResponse response = productMapper.toResponse(product);
        return RsData.ok("상품이 조회되었습니다", response);
    }

    /**
     * 상품 수정
     * - 상품 정보 수정 및 이미지 추가/삭제
     * - 경매 시작 전에만 수정 가능
     * - 본인의 상품만 수정 가능
     *
     * @param productId 수정할 상품의 ID
     * @param request 수정할 상품 정보 (변경할 필드만 포함)
     * @param images 추가할 이미지 파일 (선택)
     * @param deleteImageIds 삭제할 이미지 ID 리스트 (선택)
     * @param user 현재 로그인한 사용자
     * @return 수정된 상품 정보
     */
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

    /**
     * 상품 삭제
     * - 경매 시작 전에만 삭제 가능
     * - 본인의 상품만 삭제 가능
     * - 관련된 모든 이미지 파일도 함께 삭제됨
     *
     * @param productId 삭제할 상품의 ID
     * @param user 현재 로그인한 사용자
     */
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

    /**
     * 내 상품 목록 조회 (RDB 기반)
     * - 로그인한 사용자가 등록한 상품 목록 조회
     * - 판매 상태별 필터링 가능
     * - 낙찰자 및 리뷰 정보 포함
     *
     * @param status 판매 상태 (SELLING, SOLD, FAILED)
     */
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

    /**
     * 특정 회원의 상품 목록 조회 (RDB 기반)
     * - 다른 회원이 등록한 상품 목록 조회
     * - 판매 상태별 필터링 가능
     * - 리뷰 정보 포함
     * - 회원 프로필 페이지 등에서 사용
     *
     * @param memberId 조회할 회원의 ID
     */
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

    /**
     * Elasticsearch 검색 분석기 재로드
     * 사용자 사전, 동의어 사전 변경 후 호출 필요
     * TODO: 관리자만 접근 가능하도록 변경 필요
     */
    @PostMapping("/reload-analyzers")
//    @PreAuthorize("hasRole('ADMIN')")
    public RsData<ReloadAnalyzersResponse> reloadSearchAnalyzers() {
        return productSearchService.reloadSearchAnalyzers();
    }
}
