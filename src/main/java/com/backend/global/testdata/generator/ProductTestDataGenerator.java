package com.backend.global.testdata.generator;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
@Slf4j
public class ProductTestDataGenerator {
    
    private final ProductService productService;
    private final Faker faker = new Faker(new Locale("ko"));
    
    public List<Product> generate(int count, List<Member> sellers) {
        log.info("상품 데이터 생성 시작: {}개", count);

        List<Product> products = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            try {
                Member seller = sellers.get(faker.random().nextInt(sellers.size()));
                ProductCreateRequest request = createRandomProduct();
                
                Product product = productService.saveProduct(seller, request);
                products.add(product);
                
                if ((i + 1) % 100 == 0) {
                    log.info("진행률: {}/{}", i + 1, count);
                }
                
            } catch (Exception e) {
                log.warn("상품 생성 실패 ({}번째): {}", i + 1, e.getMessage());
            }
        }

        return products;
    }
    
    private ProductCreateRequest createRandomProduct() {
        String[] productTypes = {"아이폰", "갤럭시", "맥북", "에어팟", "아이패드", "노트북",
                "스마트워치", "태블릿", "헤드폰", "마우스", "키보드", "모니터"};
        String[] brands = {"Apple", "Samsung", "LG"};
        
        String productName = faker.options().option(productTypes) + " " +
                           faker.options().option(brands) + " " +
                           faker.number().numberBetween(1, 100);
                           
        return new ProductCreateRequest(
            productName,
            faker.lorem().paragraph(3),
            faker.number().numberBetween(1, 12),
            (long) faker.number().numberBetween(10000, 5000000),
            LocalDateTime.now().minusDays(faker.number().numberBetween(0, 30)),
            faker.options().option("24시간", "48시간"),
            faker.options().option(DeliveryMethod.values()),
            faker.address().city() + " " + faker.address().streetName()
        );
    }
}