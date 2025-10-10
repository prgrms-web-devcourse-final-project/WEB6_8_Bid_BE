package com.backend.domain.product.controller;

import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.dto.response.*;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.global.page.dto.PageDto;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 상품 관련 REST API 컨트롤러
 * - 경매 상품의 CRUD 작업을 처리
 * - RDB와 Elasticsearch 기반 검색 기능 제공
 * - 멀티파트 파일 업로드를 통한 이미지 처리
 */
@Tag(name = "Product", description = "상품 관련 API")
@RequestMapping("api/v1/products")
public interface ApiV1ProductControllerDocs {

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
    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "상품 등록 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<ProductResponse> createProduct(
            @Parameter(description = "상품 등록 요청 정보", required = true) @RequestPart("request") @Valid ProductCreateRequest request,
            @Parameter(description = "상품 이미지", required = true) @RequestPart("images") List<MultipartFile> images,
            @Parameter(description = "로그인 회원") @AuthenticationPrincipal User user
    );


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
    @Operation(summary = "상품 목록 조회", description = "조건에 따라 상품 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<PageDto<ProductListItemDto>> getProducts(
            @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "상품명 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "상품 카테고리 (번호)") @RequestParam(required = false) Integer[] category,
            @Parameter(description = "직거래 시 지역") @RequestParam(required = false) String[] location,
            @Parameter(description = "배송 가능 여부") @RequestParam(required = false) Boolean isDelivery,
            @Parameter(description = "경매 상태") @RequestParam(defaultValue = "BIDDING") AuctionStatus status,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    );


    /**
     * 상품 목록 조회 (Elasticsearch 기반)
     * - Elasticsearch의 전문 검색 기능 활용
     * - 한글 형태소 분석(nori analyzer) 지원
     * - RDB보다 빠른 검색 성능 제공
     */
    @Operation(summary = "상품 목록 조회 (ElasticSearch)", description = "ElasticSearch를 사용하여 상품 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<PageDto<ProductListItemDto>> getProductsByElasticsearch(
            @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "상품명 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "상품 카테고리 (번호)") @RequestParam(required = false) Integer[] category,
            @Parameter(description = "직거래 시 지역") @RequestParam(required = false) String[] location,
            @Parameter(description = "배송 가능 여부") @RequestParam(required = false) Boolean isDelivery,
            @Parameter(description = "경매 상태") @RequestParam(defaultValue = "BIDDING") AuctionStatus status,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    );


    /**
     * 상품 상세 조회
     * - 특정 상품의 모든 정보를 조회
     * - 이미지 목록, 판매자 정보 포함
     *
     * @param productId 조회할 상품의 ID
     * @return 상품 상세 정보
     */
    @Operation(summary = "상품 상세 조회", description = "상품을 ID로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<ProductResponse> getProduct(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId
    );


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
    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 수정 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "403", description = "인가 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<ProductResponse> modifyProduct(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId,
            @Parameter(description = "상품 수정 요청 정보", required = true) @RequestPart("request") @Valid ProductModifyRequest request,
            @Parameter(description = "상품 이미지") @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @Parameter(description = "삭제할 이미지 ID") @RequestPart(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @Parameter(description = "로그인 회원") @AuthenticationPrincipal User user
    );


    /**
     * 상품 삭제
     * - 경매 시작 전에만 삭제 가능
     * - 본인의 상품만 삭제 가능
     * - 관련된 모든 이미지 파일도 함께 삭제됨
     *
     * @param productId 삭제할 상품의 ID
     * @param user 현재 로그인한 사용자
     */
    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 삭제 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "403", description = "인가 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<Void> deleteProduct(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId,
            @Parameter(description = "로그인 회원") @AuthenticationPrincipal User user
    );


    /**
     * 내 상품 목록 조회 (RDB 기반)
     * - 로그인한 사용자가 등록한 상품 목록 조회
     * - 판매 상태별 필터링 가능
     * - 낙찰자 및 리뷰 정보 포함
     *
     * @param status 판매 상태 (SELLING, SOLD, FAILED)
     */
    @Operation(summary = "내 상품 조회", description = "내가 올린 상품들을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 상품 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<PageDto<MyProductListItemDto>> getMyProducts(
            @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "판매 상태") @RequestParam(defaultValue = "SELLING") SaleStatus status,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort,
            @Parameter(description = "로그인 회원") @AuthenticationPrincipal User user
    );


    /**
     * 특정 회원의 상품 목록 조회 (RDB 기반)
     * - 다른 회원이 등록한 상품 목록 조회
     * - 판매 상태별 필터링 가능
     * - 리뷰 정보 포함
     * - 회원 프로필 페이지 등에서 사용
     *
     * @param memberId 조회할 회원의 ID
     */
    @Operation(summary = "특정 회원 상품 조회", description = "특정 회원이 올린 상품들을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "특정 회원 상품 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<PageDto<ProductListByMemberItemDto>> getProductsByMember(
            @Parameter(description = "회원 ID", required = true) @PathVariable Long memberId,
            @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "판매 상태") @RequestParam(defaultValue = "SELLING") SaleStatus status,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    );


    /**
     * Elasticsearch 검색 분석기 재로드
     * 사용자 사전, 동의어 사전 변경 후 호출 필요
     */
    @Operation(summary = "Elasticsearch 검색 분석기 재로드", description = "Elasticsearch 검색 분석기를 재로드합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 분석기 재로드 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "500", description = "검색 분석기 재로드 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<ReloadAnalyzersResponse> reloadSearchAnalyzers();
}
