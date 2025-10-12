package com.backend.domain.product.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("STANDARD")
@NoArgsConstructor
public class StandardProduct extends Product {
    public StandardProduct(String productName, String description, ProductCategory category, Long initialPrice, LocalDateTime startTime, Integer duration, DeliveryMethod deliveryMethod, String location, Member seller) {
        super(productName, description, category, initialPrice, startTime, duration, deliveryMethod, location, seller);
    }

    // ========== ✅ 테스트 전용 빌더 ========== //
    /**
     * 테스트 전용 빌더
     * - 프로덕션 코드에서는 사용 금지
     * - ID를 포함한 모든 필드를 직접 설정 가능
     * - 단위 테스트에서 목 데이터 생성용
     */
    @Builder(builderMethodName = "testBuilder", buildMethodName = "testBuild")
    public StandardProduct(
            Long id, String productName, String description, ProductCategory category,
            Long initialPrice, Long currentPrice, LocalDateTime startTime,
            LocalDateTime endTime, Integer duration, String status,
            DeliveryMethod deliveryMethod, String location, Member seller
    ) {
        setId(id);
        initForTest(productName, description, category, initialPrice, currentPrice, startTime, endTime, duration, status, deliveryMethod, location, seller);
    }
}