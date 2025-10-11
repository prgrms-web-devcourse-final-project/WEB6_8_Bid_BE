package com.backend.domain.product.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProductServiceFactory {
    private final Map<String, ProductService> serviceMap;
    
    public ProductServiceFactory(
        @Qualifier("standardProductService") ProductService standardService,
        @Qualifier("kakaoProductService") ProductService kakaoService,
        @Qualifier("starbucksProductService") ProductService starbucksService
    ) {
        this.serviceMap = Map.of(
            "STANDARD", standardService,
            "KAKAO", kakaoService,
            "STARBUCKS", starbucksService
        );
    }
    
    public ProductService getService(String productType) {
        return serviceMap.getOrDefault(productType, serviceMap.get("STANDARD"));
    }
}