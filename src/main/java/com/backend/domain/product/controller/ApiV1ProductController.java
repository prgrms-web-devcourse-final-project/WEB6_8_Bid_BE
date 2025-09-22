package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.dto.ProductDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.service.ProductService;
import com.backend.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/products")
@RequiredArgsConstructor
public class ApiV1ProductController {
    private final ProductService productService;
    private final MemberRepository memberRepository;

    @PostMapping
    public RsData<ProductDto> createProduct(
            @RequestPart("request") @Valid ProductCreateRequest request,
            @RequestPart("images") List<MultipartFile> images
    ) {
        // TODO: JWT 토큰에서 사용자 추출
        // Member actor = rq.getActor();
        Member actor = memberRepository.findAll().getFirst();

        Product product = productService.createProduct(actor, request, images);

        return new RsData<>("201", "상품이 등록되었습니다.", ProductDto.fromEntity(product));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", "200");
        response.put("msg", "상품들이 조회되었습니다.");

        Map<String, Object> data = new HashMap<>();

        // content 배열
        List<Map<String, Object>> content = new ArrayList<>();

        // 첫 번째 상품
        Map<String, Object> product1 = new HashMap<>();
        product1.put("productId", 1);
        product1.put("name", "아이폰 15 Pro 256GB");
        product1.put("category", "디지털/가전");
        product1.put("initialPrice", 1000000);
        product1.put("currentPrice", 1200000);
        product1.put("auctionStartTime", "2024-12-17T09:00:00");
        product1.put("auctionEndTime", "2024-12-18T09:00:00");
        product1.put("auctionDuration", 24);
        product1.put("status", AuctionStatus.BIDDING.getDisplayName());
        product1.put("biddersCount", 12);
        product1.put("location", "서울 강남구");
        product1.put("thumbnailUrl", "/images/product1_1.jpg");

        Map<String, Object> seller1 = new HashMap<>();
        seller1.put("id", 2);
        seller1.put("nickname", "전자기기왕");
        seller1.put("creditScore", 75);
        seller1.put("profileImageUrl", "/images/member2.jpg");
        seller1.put("review_count", 8);
        product1.put("seller", seller1);

        content.add(product1);

        // 두 번째 상품
        Map<String, Object> product2 = new HashMap<>();
        product2.put("productId", 2);
        product2.put("name", "갤럭시 S24 Ultra 512GB");
        product2.put("category", "디지털/가전");
        product2.put("initialPrice", 1200000);
        product2.put("currentPrice", 1500000);
        product2.put("auctionStartTime", "2024-12-17T10:00:00");
        product2.put("auctionEndTime", "2024-12-18T10:00:00");
        product2.put("auctionDuration", 24);
        product2.put("status", AuctionStatus.BIDDING.getDisplayName());
        product2.put("biddersCount", 5);
        product2.put("location", null);
        product2.put("thumbnailUrl", "/images/product2_1.jpg");

        product2.put("seller", seller1);

        content.add(product2);

        // 세 번째 상품
        Map<String, Object> product3 = new HashMap<>();
        product3.put("productId", 3);
        product3.put("name", "구찌 GG 마몽 숄더백");
        product3.put("category", "패션/의류");
        product3.put("initialPrice", 800000);
        product3.put("currentPrice", 800000);
        product3.put("auctionStartTime", "2024-12-17T11:00:00");
        product3.put("auctionEndTime", "2024-12-19T11:00:00");
        product3.put("auctionDuration", 48);
        product3.put("status", AuctionStatus.BEFORE_START.getDisplayName());
        product3.put("biddersCount", 0);
        product3.put("location", "부산 해운대");
        product3.put("thumbnailUrl", "/images/product3_1.jpg");

        Map<String, Object> seller2 = new HashMap<>();
        seller2.put("id", 3);
        seller2.put("nickname", "명품러버");
        seller2.put("creditScore", 80);
        seller2.put("profileImageUrl", "/images/member3.jpg");
        seller2.put("review_count", 15);
        product3.put("seller", seller2);

        content.add(product3);

        data.put("content", content);

        // pageable 정보
        Map<String, Object> pageable = new HashMap<>();
        pageable.put("currentPage", page);
        pageable.put("pageSize", size);
        pageable.put("totalPages", 10);
        pageable.put("totalElements", 95);
        pageable.put("hasNext", true);
        pageable.put("hasPrevious", false);

        data.put("pageable", pageable);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
