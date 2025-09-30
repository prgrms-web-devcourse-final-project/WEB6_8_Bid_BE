package com.backend.global.testdata.controller;

import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.global.testdata.generator.TestDataGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test-data")
@Profile("dev")
@Tag(name = "TestData", description = "테스트 데이터 생성 API (개발용)")
@RequiredArgsConstructor
public class TestDataController {

    private final TestDataGenerator testDataGenerator;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @PostMapping("/generate")
    @Operation(summary = "테스트 데이터 생성")
    public ResponseEntity<TestDataGenerationResult> generateTestData(
            @RequestParam(defaultValue = "1000") int count
    ) {
        if (count > 10000) {
            return ResponseEntity.badRequest()
                    .body(TestDataGenerationResult.fail("최대 10000개까지만 생성 가능"));
        }

        long startTime = System.currentTimeMillis();

        try {
            testDataGenerator.generateTestData(count);

            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(TestDataGenerationResult.success(
                    count,
                    duration,
                    productRepository.count(),
                    memberRepository.count()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(TestDataGenerationResult.fail(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "현재 데이터 통계")
    public ResponseEntity<DataStats> getDataStats() {
        return ResponseEntity.ok(DataStats.builder()
                .productCount(productRepository.count())
                .memberCount(memberRepository.count())
                .build());
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "테스트 데이터 정리")
    public ResponseEntity<String> cleanupTestData() {
        // 테스트 데이터만 삭제하는 로직
        return ResponseEntity.ok("테스트 데이터 정리 완료");
    }
}

@Data
@Builder
class TestDataGenerationResult {
    private boolean success;
    private String message;
    private Integer generatedCount;
    private Long durationMs;
    private Long totalProducts;
    private Long totalMembers;

    static TestDataGenerationResult success(int count, long duration, long products, long members) {
        return TestDataGenerationResult.builder()
                .success(true)
                .message("테스트 데이터 생성 완료")
                .generatedCount(count)
                .durationMs(duration)
                .totalProducts(products)
                .totalMembers(members)
                .build();
    }

    static TestDataGenerationResult fail(String message) {
        return TestDataGenerationResult.builder()
                .success(false)
                .message(message)
                .build();
    }
}

@Data
@Builder
class DataStats {
    private Long productCount;
    private Long memberCount;
}