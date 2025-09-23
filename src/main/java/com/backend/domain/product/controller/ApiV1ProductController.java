package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.dto.*;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.service.ProductService;
import com.backend.global.rsData.RsData;
import com.backend.standard.page.dto.PageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public RsData<ProductDto> createProduct(
            @RequestPart("request") @Valid ProductCreateRequest request,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal Member actor
    ) {
        Product product = productService.create(actor, request, images);

        return new RsData<>("201", "상품이 등록되었습니다.", ProductDto.fromEntity(product));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public RsData<PageDto<ProductListDto>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer[] category,
            @RequestParam(required = false) String[] location,
            @RequestParam(required = false) Boolean isDelivery,
            @RequestParam(defaultValue = "BIDDING") AuctionStatus status,
            @RequestParam(defaultValue = "LATEST") ProductSearchSortType sort
    ) {
        ProductSearchDto search = new ProductSearchDto(keyword, category, location, isDelivery, status);
        Page<Product> products = productService.findBySearchPaged(page, size, sort, search);

        return new RsData<>(
                "200",
                "상품 목록이 조회되었습니다.",
                PageDto.fromPage(
                        products.map(ProductListDto::fromEntity)
                )
        );
    }

    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public RsData<ProductDto> getProduct(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);

        return new RsData<>(
                "200",
                "상품이 조회되었습니다.",
                ProductDto.fromEntity(product)
        );
    }

    @PutMapping("/{productId}")
    public RsData<ProductDto> modifyProduct(
            @PathVariable Long productId,
            @RequestPart("request") @Valid ProductModifyRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @AuthenticationPrincipal Member actor
    ) {
        Product product = productService.getProductById(productId);

        product.checkActorCanModify(actor);

        productService.modifyProduct(product, request, images, deleteImageIds);

        return new RsData<>(
                "200",
                "상품이 수정되었습니다.",
                ProductDto.fromEntity(product)
        );
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @PathVariable Long productId
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", "200");
        response.put("msg", "상품이 삭제되었습니다.");
        response.put("data", null);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "LATEST") String sort
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
        product1.put("currentPrice", 2000000);
        product1.put("auctionStartTime", "2024-12-17T09:00:00");
        product1.put("auctionEndTime", "2024-12-18T09:00:00");
        product1.put("auctionDuration", 24);
        product1.put("status", AuctionStatus.SUCCESSFUL.getDisplayName());
        product1.put("biddersCount", 12);
        product1.put("location", "서울 강남구");
        product1.put("thumbnailUrl", "/images/product1_1.jpg");

        Map<String, Object> bidder = new HashMap<>();
        bidder.put("id", 4);
        bidder.put("nickname", "낙찰자");
        bidder.put("profileImageUrl", "/images/member4.jpg");
        bidder.put("phoneNumber", "010-0000-0000");
        product1.put("bidder", bidder);

        Map<String, Object> review = new HashMap<>();
        review.put("id", 4);
        review.put("reviewerNickName", "낙찰자");
        review.put("productName", "아이폰 15 Pro 256GB");
        review.put("comment", "상품 상태 양호합니다! 판매자님이 친절하셔서 좋았습니다.");
        review.put("isSatisfied", true);
        product1.put("review", review);

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

    @GetMapping("/members/{memberId}")
    public ResponseEntity<Map<String, Object>> getProductsByMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "LATEST") String sort
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", "200");
        response.put("msg", "판매자 상품들이 조회되었습니다.");

        Map<String, Object> data = new HashMap<>();

        // content 배열
        List<Map<String, Object>> content = new ArrayList<>();

        // 첫 번째 상품
        Map<String, Object> product1 = new HashMap<>();
        product1.put("productId", 1);
        product1.put("name", "아이폰 15 Pro 256GB");
        product1.put("category", "디지털/가전");
        product1.put("initialPrice", 1000000);
        product1.put("currentPrice", 2000000);
        product1.put("auctionStartTime", "2024-12-17T09:00:00");
        product1.put("auctionEndTime", "2024-12-18T09:00:00");
        product1.put("auctionDuration", 24);
        product1.put("status", AuctionStatus.SUCCESSFUL.getDisplayName());
        product1.put("biddersCount", 12);
        product1.put("location", "서울 강남구");
        product1.put("thumbnailUrl", "/images/product1_1.jpg");

        Map<String, Object> review = new HashMap<>();
        review.put("id", 4);
        review.put("reviewerNickName", "낙찰자");
        review.put("productName", "아이폰 15 Pro 256GB");
        review.put("comment", "상품 상태 양호합니다! 판매자님이 친절하셔서 좋았습니다.");
        review.put("isSatisfied", true);
        product1.put("review", review);

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
