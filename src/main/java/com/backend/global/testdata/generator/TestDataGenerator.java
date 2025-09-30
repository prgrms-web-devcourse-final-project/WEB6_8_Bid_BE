package com.backend.global.testdata.generator;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
@Slf4j
public class TestDataGenerator {
    
    private final ProductTestDataGenerator productGenerator;
    private final MemberTestDataGenerator memberGenerator;
    
    @Transactional
    public void generateTestData(int productCount) {
        log.info("=== 테스트 데이터 생성 시작 ===");
        
        // 1. 회원 먼저 생성
        List<Member> members = memberGenerator.generate(20);
        log.info("회원 {}명 생성 완료", members.size());
        
        // 2. 상품 생성
        List<Product> products = productGenerator.generate(productCount, members);
        log.info("상품 {}개 생성 완료", products.size());
        
        log.info("=== 테스트 데이터 생성 완료 ===");
    }
}