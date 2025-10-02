package com.backend.domain.product.document;

import com.backend.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

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
    
    // Entity -> Document 변환
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