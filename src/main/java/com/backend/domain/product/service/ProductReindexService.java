package com.backend.domain.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // copyDataToNewIndex 메서드 때문
public class ProductReindexService {
    private final ElasticsearchClient elasticsearchClient;
    private final ProductRepository productRepository;
    
    private static final String ALIAS = "products";
    private static final String INDEX_PREFIX = "products_v";

    // ======================================= public methods ======================================= //
    /**
     * 무중단 재인덱싱 (배포 환경용)
     * 
     * 순서:
     * 1. 새 인덱스 생성 (products_v{timestamp})
     * 2. 데이터 복사
     * 3. Alias 전환
     * 4. 구 인덱스 유지 (롤백용)
     *
     * @return 결과 메시지
     */
    public String reindexWithZeroDowntime() {
        try {
            // 1. 현재 alias가 가리키는 인덱스 확인
            String oldIndex = getCurrentIndex();
            log.info("현재 인덱스: {}", oldIndex);
            
            // 2. 새 인덱스명 생성
            String newIndex = generateNewIndexName();
            log.info("새 인덱스 생성: {}", newIndex);
            
            // 3. 새 인덱스 생성
            createNewIndex(newIndex);
            
            // 4. 데이터 복사 (RDB → Elasticsearch)
            int count = copyDataToNewIndex(newIndex);
            log.info("데이터 복사 완료: {}건", count);
            
            // 5. Alias 전환
            switchAlias(oldIndex, newIndex);
            log.info("Alias 전환 완료: {} → {}", oldIndex, newIndex);
            
            return String.format("재인덱싱 완료! %s → %s (%d건)", oldIndex, newIndex, count);
            
        } catch (Exception e) {
            log.error("재인덱싱 실패", e);
            throw ServiceException.badRequest("재인덱싱 실패: " + e.getMessage());
        }
    }

    /**
     * 구 인덱스 삭제 (롤백 기간 후)
     * @param indexName 삭제할 인덱스명
     */
    @Transactional
    public void deleteOldIndex(String indexName) throws IOException {
        elasticsearchClient.indices().delete(d -> d.index(indexName));
        log.info("구 인덱스 삭제: {}", indexName);
    }

    // ======================================= helper methods ======================================= //
    /**
     * 현재 alias가 가리키는 인덱스 조회
     */
    private String getCurrentIndex() throws IOException {
        GetAliasResponse response = elasticsearchClient.indices()
                .getAlias(a -> a.name(ALIAS));
        
        return response.result().keySet().stream()
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 새 인덱스명 생성
     * 예: products_v20251006_143000
     */
    private String generateNewIndexName() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return INDEX_PREFIX + timestamp;
    }
    
    /**
     * 새 인덱스 생성 (설정 + 매핑 적용)
     */
    private void createNewIndex(String indexName) throws IOException {
        String settings = loadResourceFile("elasticsearch/product-settings.json");
        String mappings = loadResourceFile("elasticsearch/product-mappings.json");
        
        // 인덱스 생성
        CreateIndexRequest request = CreateIndexRequest.of(b -> b
                .index(indexName)
                .settings(s -> s.withJson(new java.io.StringReader(settings)))
                .mappings(m -> m.withJson(new java.io.StringReader(mappings)))
        );
        
        elasticsearchClient.indices().create(request);
        log.info("인덱스 생성 완료: {}", indexName);
    }
    
    /**
     * RDB에서 데이터를 읽어 새 인덱스에 복사
     */
    private int copyDataToNewIndex(String indexName) {
        int batchSize = 100;
        int totalCount = 0;
        int page = 0;
        
        Page<Product> productPage;
        
        do {
            productPage = productRepository.findAll(PageRequest.of(page, batchSize));
            
            for (Product product : productPage.getContent()) {
                ProductDocument document = ProductDocument.fromEntity(product);
                
                // 특정 인덱스에 직접 저장
                try {
                    elasticsearchClient.index(i -> i
                            .index(indexName)
                            .id(String.valueOf(document.getProductId()))
                            .document(document)
                    );
                    totalCount++;
                } catch (IOException e) {
                    log.error("문서 인덱싱 실패: {}", product.getId(), e);
                }
            }
            
            page++;
            
            if (page % 10 == 0) {
                log.info("진행: {}건 인덱싱 완료", totalCount);
            }
            
        } while (productPage.hasNext());
        
        return totalCount;
    }
    
    /**
     * Alias 원자적 전환
     */
    private void switchAlias(String oldIndex, String newIndex) throws IOException {
        UpdateAliasesRequest request = UpdateAliasesRequest.of(b -> b
                .actions(a -> a.remove(r -> r.index(oldIndex).alias(ALIAS)))
                .actions(a -> a.add(add -> add.index(newIndex).alias(ALIAS)))
        );
        
        elasticsearchClient.indices().updateAliases(request);
    }
    
    /**
     * 리소스 파일 읽기
     */
    private String loadResourceFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return Files.readString(resource.getFile().toPath());
    }
}