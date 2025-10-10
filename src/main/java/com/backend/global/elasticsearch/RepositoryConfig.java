package com.backend.global.elasticsearch;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.backend.domain.*.repository"
)
@EnableElasticsearchRepositories(
        basePackages = "com.backend.domain.product.repository.elasticsearch"  // 별도 패키지
)
public class RepositoryConfig {
}
