package com.backend.domain.product.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.dto.request.ProductModifyRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.exception.ProductException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * 상품(Product) 비즈니스 로직 처리 서비스
 * - 상품의 생성, 조회, 수정, 삭제 기능 제공
 * - 이미지 처리 및 Elasticsearch 동기화는 각각의 서비스에 위임
 * - 권한 검증 및 비즈니스 규칙 적용
 */
@Service
public interface ProductService {
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
    Product createProduct(Member actor, ProductCreateRequest request, List<MultipartFile> images);

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
    Page<Product> findBySearchPaged(
            int page, int size, ProductSearchSortType sort, ProductSearchDto search
    );

    /**
     * 특정 회원의 상품 목록 조회 (페이징)
     * - QueryDSL을 사용한 동적 쿼리 생성
     * - 회원이 등록한 상품을 판매 상태별로 조회
     * - 내 상품 보기 또는 특정 판매자의 상품 목록에 사용
     *
     * @param actor 조회할 회원
     * @param status 판매 상태 (SELLING, SOLD, FAILED)
     */
    Page<Product> findByMemberPaged(
            int page, int size, ProductSearchSortType sort, Member actor, SaleStatus status
    );

    Optional<Product> findById(Long productId);

    default Product getProductById(Long productId) {
        return findById(productId).orElseThrow(ProductException::notFound);
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
    Product modifyProduct(Product product, ProductModifyRequest request, List<MultipartFile> images, List<Long> deleteImageIds);

    /**
     * 상품 삭제
     * - 경매 시작 전에만 삭제 가능
     * - 모든 이미지 파일 삭제
     * - Elasticsearch 인덱스에서도 제거
     *
     * @param product 삭제할 상품
     * @throws ProductException 경매 시작 후 삭제 시도
     */
    void deleteProduct(Product product);
}
