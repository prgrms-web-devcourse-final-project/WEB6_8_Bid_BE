package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.*;
import com.backend.domain.product.exception.ProductException;
import com.backend.domain.product.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 상품(Product) 비즈니스 로직 처리 서비스
 * - 상품의 생성, 조회, 수정, 삭제 기능 제공
 * - 이미지 처리 및 Elasticsearch 동기화는 각각의 서비스에 위임
 * - 권한 검증 및 비즈니스 규칙 적용
 */
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductImageService productImageService;
    private final ProductRepository productRepository;
    private final ProductSyncService productSyncService;

    // ======================================= create methods ======================================= //
    /**
     * 새로운 상품 생성
     * - 상품 정보 검증 및 저장
     * - 이미지 업로드 처리
     * - Elasticsearch 인덱싱
     *
     * @param actor 상품을 등록하는 회원
     * @param request 상품 등록 요청 정보
     * @param images 상품 이미지 파일 리스트 (1~5개)
     * @return 생성된 상품 엔티티
     * @throws ProductException 직거래 시 위치 미입력, 이미지 검증 실패 등
     */
    @Transactional
    public Product createProduct(Member actor, ProductCreateRequest request, List<MultipartFile> images) {
        // location 검증
        validateLocation(request.location(), request.deliveryMethod());

        // Product 생성 및 저장
        Product savedProduct = saveProduct(actor, request);

        // 이미지 검증 및 저장
        productImageService.validateAndCreateImages(savedProduct, images);

        // Elasticsearch 동기화
        productSyncService.syncProductCreation(savedProduct);

        return savedProduct;
    }

    public Product saveProduct(Member actor, ProductCreateRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                ProductCategory.fromId(request.categoryId()),
                request.initialPrice(),
                request.auctionStartTime(),
                AuctionDuration.fromValue(request.auctionDuration()),
                request.deliveryMethod(),
                request.location(),
                actor
        );
        return productRepository.save(product);
    }

    // ======================================= find/get methods ======================================= //
    /**
     * 검색 조건에 따른 상품 목록 조회 (페이징)
     * - QueryDSL을 사용한 동적 쿼리 생성
     * - 키워드(상품명), 카테고리, 지역, 배송 가능 여부, 경매 상태로 필터링
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 기준 (LATEST, PRICE_HIGH, PRICE_LOW, ENDING_SOON, POPULAR)
     * @param search 검색 조건 (keyword, category, location, isDelivery, status)
     * @return 페이징된 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<Product> findBySearchPaged(
            int page, int size, ProductSearchSortType sort, ProductSearchDto search
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productRepository.findBySearchPaged(pageable, search);
    }

    /**
     * 특정 회원의 상품 목록 조회 (페이징)
     * - QueryDSL을 사용한 동적 쿼리 생성
     * - 회원이 등록한 상품을 판매 상태별로 조회
     * - 내 상품 보기 또는 특정 판매자의 상품 목록에 사용
     *
     * @param actor 조회할 회원
     * @param status 판매 상태 (SELLING, SOLD, FAILED)
     */
    @Transactional(readOnly = true)
    public Page<Product> findByMemberPaged(
            int page, int size, ProductSearchSortType sort, Member actor, SaleStatus status
    ) {
        Pageable pageable = getPageable(page, size, sort);
        return productRepository.findByMemberPaged(pageable, actor.getId(), SaleStatus.fromSaleStatus(status));
    }

    /**
     * Pageable 객체 생성 및 검증
     * - page: 1 이상 (기본값: 1)
     * - size: 1~100 (기본값: 20)
     * - sort: ProductSearchSortType을 Spring Data Sort로 변환
     */
    private Pageable getPageable(int page, int size, ProductSearchSortType sort) {
        page = (page > 0) ? page : 1;
        size = (size > 0 && size <= 100) ? size : 20;
        return PageRequest.of(page - 1, size, sort.toSort());
    }

    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    public Product getProductById(Long productId) {
        return findById(productId).orElseThrow(ProductException::notFound);
    }

    public Optional<Product> findLatest() {
        return productRepository.findFirstByOrderByIdDesc();
    }

    public long count() {
        return productRepository.count();
    }

    // ======================================= modify/delete methods ======================================= //
    /**
     * 상품 정보 수정
     * - 경매 시작 전에만 수정 가능
     * - 이미지 추가/삭제 처리
     * - Elasticsearch 문서 업데이트
     *
     * @param product 수정할 상품
     * @param request 수정 요청 정보 (변경할 필드만 포함)
     * @param images 추가할 이미지 파일 (null 가능)
     * @param deleteImageIds 삭제할 이미지 ID 리스트 (null 가능)
     * @return 수정된 상품
     * @throws ProductException 경매 시작 후 수정 시도, 이미지 검증 실패 등
     */
    @Transactional
    public Product modifyProduct(Product product, ProductModifyRequest request, List<MultipartFile> images, List<Long> deleteImageIds) {
        // 수정 요청 검증
        ProductModifyRequest validatedRequest = validateModifyRequest(product, request);

        // Product 수정
        product.modify(validatedRequest);

        // 이미지 검증 및 수정
        productImageService.validateAndModifyImages(product, images, deleteImageIds);

        // Elasticsearch 동기화
        productSyncService.syncProductUpdate(product);

        return product;
    }

    /**
     * 상품 삭제
     * - 경매 시작 전에만 삭제 가능
     * - 모든 이미지 파일 삭제
     * - Elasticsearch 인덱스에서도 제거
     *
     * @param product 삭제할 상품
     * @throws ProductException 경매 시작 후 삭제 시도
     */
    @Transactional
    public void deleteProduct(Product product) {
        // 삭제 권한 검증
        if (product.getStartTime().isBefore(LocalDateTime.now())) {
            throw ProductException.auctionDeleteForbidden();
        }

        // 이미지 및 Product 삭제
        productImageService.deleteAllProductImages(product);
        productRepository.delete(product);

        // Elasticsearch 동기화
        productSyncService.syncProductDeletion(product.getId());
    }

    // ======================================= validation methods ======================================= //
    /**
     * 배송 방법에 따른 위치 정보 검증
     * - 직거래 또는 직거래+택배 선택 시 위치 필수
     *
     * @throws ProductException 위치 정보가 없을 경우
     */
    private void validateLocation(String location, DeliveryMethod deliveryMethod) {
        if (deliveryMethod == DeliveryMethod.TRADE || deliveryMethod == DeliveryMethod.BOTH) {
            if (location == null || location.isBlank()) {
                throw ProductException.locationRequired();
            }
        }
    }

    /**
     * 상품 수정 요청 검증
     * - 경매 시작 후 수정 불가 검증
     * - 배송 방법에 따른 위치 정보 검증
     *
     * @param product 기존 상품 정보
     * @param request 수정 요청 정보
     * @return 검증 및 최적화된 수정 요청 객체
     * @throws ProductException 경매 시작 후 수정 시도, 위치 정보 누락 등
     */
    public ProductModifyRequest validateModifyRequest(Product product, ProductModifyRequest request) {
        // 경매 시작 후 수정 불가
        if (product.getStartTime().isBefore(LocalDateTime.now())) {
            throw ProductException.auctionModifyForbidden();
        }

        // 배송 방법 변경 시 위치 정보 검증
        validateLocation(request.location(), request.deliveryMethod());

        // request 필드가 null일 경우, 기존 객체의 필드로 덮어쓰기
        return new ProductModifyRequest(
                (request.name() != null) ? request.name() : product.getProductName(),
                (request.description() != null) ? request.description() : product.getDescription(),
                (request.categoryId() != null) ? request.categoryId() : product.getCategory().getId(),
                (request.initialPrice() != null) ? request.initialPrice() : product.getInitialPrice(),
                (request.auctionStartTime() != null) ? request.auctionStartTime() : product.getStartTime(),
                (request.auctionDuration() != null) ? request.auctionDuration() : getDurationString(product),
                (request.deliveryMethod() != null) ? request.deliveryMethod() : product.getDeliveryMethod(),
                (request.location() != null) ? request.location() : product.getLocation()
        );
    }

    private String getDurationString(Product product) {
        return product.getDuration() == 24 ? "24시간" : "48시간";
    }
}
