package com.backend.domain.product.controller;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.backend.domain.product.entity.Product;
import com.backend.global.rsData.RsData;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final EntityManager entityManager;

    // 테스트용: 경매 종료 시간을 30초 후로 설정 (PUT)
    @PutMapping("/products/{productId}/end-soon")
    @Transactional
    public RsData<String> setAuctionEndSoonPut(@PathVariable Long productId) {
        return processEndSoon(productId);
    }

    // 테스트용: 경매 종료 시간을 30초 후로 설정 (GET)
    @GetMapping("/products/{productId}/end-soon")
    @Transactional
    public RsData<String> setAuctionEndSoonGet(@PathVariable Long productId) {
        return processEndSoon(productId);
    }

    private RsData<String> processEndSoon(Long productId) {
        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            return new RsData<>("404", "상품을 찾을 수 없습니다.", null);
        }

        // 30초 후로 경매 종료 시간 설정
        product.setEndTime(LocalDateTime.now().plusSeconds(30));
        entityManager.merge(product);

        return new RsData<>("200", 
            "상품 " + productId + "의 경매 종료 시간을 30초 후로 설정했습니다. " +
            "종료 시간: " + product.getEndTime(), 
            "OK");
    }

    // 테스트용: 스케줄러 수동 실행
    @PutMapping("/scheduler/run")
    public RsData<String> runSchedulerManually() {
        // 간단한 테스트를 위해 안내 메시지만 반환
        return new RsData<>("200", 
            "스케줄러는 1분마다 자동 실행됩니다. " +
            "경매 종료 시간을 30초 후로 설정하고 1분 정도 기다려보세요.", 
            "OK");
    }
}
