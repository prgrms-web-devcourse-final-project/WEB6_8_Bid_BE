package com.backend.domain.product.controller;

import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.dto.ProductDto;
import com.backend.domain.product.dto.ProductListDto;
import com.backend.domain.product.dto.ProductModifyRequest;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.global.rsData.RsData;
import com.backend.standard.page.dto.PageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Product", description = "상품 관련 API")
@RequestMapping("api/v1/products")
public interface ApiV1ProductControllerDocs {
    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "상품 등록 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<ProductDto> createProduct(
            @Parameter(description = "상품 등록 요청 정보", required = true) @RequestPart("request") @Valid ProductCreateRequest request,
            @Parameter(description = "상품 이미지", required = true) @RequestPart("images") List<MultipartFile> images
    );


    @Operation(summary = "상품 목록 조회", description = "조건에 따라 상품 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<PageDto<ProductListDto>> getProducts(
            @Parameter(description = "페이지 번호 (1부터 시작)", required = true) @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", required = true) @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "상품명 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "상품 카테고리 (번호)") @RequestParam(required = false) Integer[] category,
            @Parameter(description = "직거래 시 지역") @RequestParam(required = false) String[] location,
            @Parameter(description = "배송 가능 여부") @RequestParam(required = false) Boolean isDelivery,
            @Parameter(description = "경매 상태") @RequestParam(defaultValue = "BIDDING") AuctionStatus status,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    );


    @Operation(summary = "상품 상세 조회", description = "상품을 ID로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = RsData.class))),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = RsData.class)))
    })
    RsData<ProductDto> getProduct(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId
    );


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
    RsData<ProductDto> modifyProduct(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId,
            @Parameter(description = "상품 수정 요청 정보", required = true) @RequestPart("request") @Valid ProductModifyRequest request,
            @Parameter(description = "상품 이미지") @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @Parameter(description = "삭제할 이미지 ID") @RequestPart(value = "deleteImageIds", required = false) List<Long> deleteImageIds
    );


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
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId
    );


    @Operation(summary = "내 상품 조회", description = "내가 올린 상품들을 조회합니다.")
    RsData<PageDto<ProductListDto>> getMyProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SELLING") SaleStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    );


    @Operation(summary = "특정 회원 상품 조회", description = "특정 회원이 올린 상품들을 조회합니다.")
    ResponseEntity<Map<String, Object>> getProductsByMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "LATEST") String sort
    );
}
