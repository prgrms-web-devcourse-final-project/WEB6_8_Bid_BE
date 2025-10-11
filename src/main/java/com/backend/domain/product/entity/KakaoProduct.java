package com.backend.domain.product.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("KAKAO")
@Getter
@NoArgsConstructor
public class KakaoProduct extends Product {

    // 카카오 전용 필드만 추가
    @Column(name = "instant_buy_price")
    private Long instantBuyPrice;

    @Column(name = "kakao_delivery_code")
    private String kakaoDeliveryCode;

    @Column(name = "commission_rate")
    private Double commissionRate = 0.05;


    public KakaoProduct(
            String productName, String description, ProductCategory category, Long initialPrice, LocalDateTime startTime, Integer duration, DeliveryMethod deliveryMethod, String location, Member seller,
            Long instantBuyPrice
    ) {
        super(productName, description, category, initialPrice, startTime, duration, deliveryMethod, location, seller);
        this.instantBuyPrice = instantBuyPrice;
    }

    public Long calculateFinalPrice(Member buyer) {
        // 카카오는 수수료 추가
        Long basePrice = getCurrentPrice();
        Long commission = (long) (basePrice * commissionRate);
        return basePrice + commission;
    }

    public String getDeliveryInfo() {
        return "카카오 자동배송: " + kakaoDeliveryCode;
    }
}
