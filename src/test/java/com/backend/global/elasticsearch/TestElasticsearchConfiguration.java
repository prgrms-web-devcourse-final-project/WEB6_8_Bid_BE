package com.backend.global.elasticsearch;

import com.backend.domain.product.repository.elasticsearch.ProductElasticRepository;
import com.backend.domain.product.service.ProductSearchService;
import com.backend.domain.product.service.ProductSyncService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestElasticsearchConfiguration {
    @Bean
    @Primary
    public ProductSyncService productSyncService() {
        return mock(ProductSyncService.class);
    }
    
    @Bean
    @Primary
    public ProductSearchService productSearchService() {
        return mock(ProductSearchService.class);
    }
    
    @Bean
    @Primary
    public ProductElasticRepository productElasticRepository() {
        return mock(ProductElasticRepository.class);
    }
}