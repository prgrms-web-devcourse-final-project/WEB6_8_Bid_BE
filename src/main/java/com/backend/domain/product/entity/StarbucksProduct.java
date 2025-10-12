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
@DiscriminatorValue("STARBUCKS")
@Getter
@NoArgsConstructor
public class StarbucksProduct extends Product {

    // 스타벅스 전용 필드만 추가
    @Column(name = "store_pickup_location")
    private String storePickupLocation;
    
    @Column(name = "coupon_code")
    private String couponCode;
    
    @Column(name = "commission_rate")
    private Double commissionRate = 0.03;
    
    public StarbucksProduct(
            String productName, String description, ProductCategory category, Long initialPrice, LocalDateTime startTime, Integer duration, DeliveryMethod deliveryMethod, String location, Member seller,
            String storePickupLocation
    ) {
        super(productName, description, category, initialPrice, startTime, duration, deliveryMethod, location, seller);
        this.storePickupLocation = storePickupLocation;
    }
    
    public Long calculateFinalPrice(Member buyer) {
        // 스타벅스는 수수료 추가 + 쿠폰 할인 적용
        Long basePrice = getCurrentPrice();
        Long discount = calculateCouponDiscount(buyer);
        long commission = (long) (basePrice * commissionRate);
        return basePrice - discount + commission;
    }
    
    public String getDeliveryInfo() {
        return "매장 픽업: " + storePickupLocation;
    }
    
    private Long calculateCouponDiscount(Member buyer) {
        if (couponCode != null) {
            return getCurrentPrice() * 10 / 100;
        }
        return 0L;
    }
}
