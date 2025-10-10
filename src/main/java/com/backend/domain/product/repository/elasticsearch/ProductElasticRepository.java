package com.backend.domain.product.repository.elasticsearch;

import com.backend.domain.product.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductElasticRepository extends ElasticsearchRepository<ProductDocument, String>, ProductElasticRepositoryCustom {
}
