package com.backend.domain.product.document;

import com.backend.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

/**
 * Elasticsearch 상품 문서
 * - RDB의 Product 엔티티를 검색용으로 최적화한 문서 구조
 * - 전문 검색, 복잡한 필터링, 빠른 정렬에 사용
 * - RDB와 실시간 동기화 (이벤트 기반)
 *
 * 인덱스 구성:
 * - 인덱스명: products
 * - 설정 파일: elasticsearch/product-settings.json (nori analyzer 설정)
 * - 매핑 파일: elasticsearch/product-mappings.json (필드 타입 정의)
 *
 * 한글 검색 최적화:
 * - nori_analyzer: 한글 형태소 분석기
 * - 품사 태그 필터링으로 불필요한 조사/어미 제거
 * - mixed 모드: 복합명사 분해 및 원형 모두 인덱싱
 *
 * 주요 검색 필드:
 * - productName: 상품명 (text)
 * - location: 거래 지역 (text)
 * - category: 카테고리 (keyword, exact match)
 * - status: 경매 상태 (keyword, exact match)
 *
 * 정렬 필드:
 * - createDate: 최신순
 * - currentPrice: 가격순
 * - endTime: 마감 임박순
 * - bidderCount: 인기순
 */
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
@Mapping(mappingPath = "elasticsearch/product-mappings.json")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {
    @Id
    private String id;  // Elasticsearch는 String ID 사용
    
    @Field(type = FieldType.Long)
    private Long productId;
    
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String productName;
    
    @Field(type = FieldType.Text, index = false)
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Long)
    private Long initialPrice;
    
    @Field(type = FieldType.Long)
    private Long currentPrice;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime startTime;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime endTime;
    
    @Field(type = FieldType.Integer)
    private Integer duration;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Keyword)
    private String deliveryMethod;
    
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String location;
    
    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;
    
    @Field(type = FieldType.Long)
    private Long sellerId;
    
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String sellerNickname;
    
    @Field(type = FieldType.Long)
    private Integer bidderCount;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createDate;

    /**
     * Entity → Document 변환 (정적 팩토리 메서드)
     * - RDB에서 조회한 Product를 Elasticsearch 문서로 변환
     * - 인덱싱 및 재인덱싱 시 사용
     *
     * 변환 규칙:
     * - Enum은 name()으로 변환 (category, deliveryMethod)
     * - 연관 엔티티는 ID와 주요 필드만 추출 (seller)
     * - 컬렉션은 제외 (bids, productImages)
     *
     * @param product 변환할 상품 엔티티
     * @return Elasticsearch에 저장할 문서
     */
    public static ProductDocument fromEntity(Product product) {
        return ProductDocument.builder()
                .id(String.valueOf(product.getId()))
                .productId(product.getId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .category(product.getCategory().name())
                .initialPrice(product.getInitialPrice())
                .currentPrice(product.getCurrentPrice())
                .startTime(product.getStartTime())
                .endTime(product.getEndTime())
                .duration(product.getDuration())
                .status(product.getStatus())
                .deliveryMethod(product.getDeliveryMethod().name())
                .location(product.getLocation())
                .thumbnailUrl(product.getThumbnail())
                .sellerId(product.getSeller().getId())
                .sellerNickname(product.getSeller().getNickname())
                .bidderCount(product.getBidderCount())
                .createDate(product.getCreateDate())
                .build();
    }
}