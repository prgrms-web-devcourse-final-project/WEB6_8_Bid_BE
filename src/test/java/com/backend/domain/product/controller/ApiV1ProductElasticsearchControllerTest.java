package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.document.ProductDocument;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.enums.SaleStatus;
import com.backend.domain.product.service.ProductSearchService;
import com.backend.domain.product.service.ProductService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.elasticsearch.uris=localhost:9200",
        "spring.data.elasticsearch.repositories.enabled=true",
        "spring.autoconfigure.exclude="  // 빈 값으로 오버라이드
})
class ApiV1ProductElasticsearchControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("상품 목록 조회")
    void getProductsByElasticsearch() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/products/es")
                ).andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("getProductsByElasticsearch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("상품 목록이 조회되었습니다"))
                .andExpect(jsonPath("$.data.pageable.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.data.pageable.totalPages").value(productPage.getTotalPages()))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.pageable.hasNext").value(productPage.hasNext()))
                .andExpect(jsonPath("$.data.pageable.hasPrevious").value(productPage.hasPrevious()));

        List<ProductDocument> products = productPage.getContent();
        resultActions.andExpect(jsonPath("$.data.content.length()").value(products.size()));

        for (int i = 0; i < products.size(); i++) {
            ProductDocument product = products.get(i);
            resultActions
                    .andExpect(jsonPath("$.data.content[%d].productId".formatted(i)).value(product.getId()))
                    .andExpect(jsonPath("$.data.content[%d].name".formatted(i)).value(product.getProductName()))
                    .andExpect(jsonPath("$.data.content[%d].category".formatted(i)).value(ProductCategory.fromName(product.getCategory())))
                    .andExpect(jsonPath("$.data.content[%d].initialPrice".formatted(i)).value(product.getInitialPrice()))
                    .andExpect(jsonPath("$.data.content[%d].currentPrice".formatted(i)).value(product.getCurrentPrice()))
                    .andExpect(jsonPath("$.data.content[%d].auctionStartTime".formatted(i)).value(Matchers.startsWith(product.getStartTime().toString().substring(0, 15))))
                    .andExpect(jsonPath("$.data.content[%d].auctionEndTime".formatted(i)).value(Matchers.startsWith(product.getEndTime().toString().substring(0, 15))))
                    .andExpect(jsonPath("$.data.content[%d].auctionDuration".formatted(i)).value(product.getDuration()))
                    .andExpect(jsonPath("$.data.content[%d].status".formatted(i)).value(product.getStatus()))
                    .andExpect(jsonPath("$.data.content[%d].bidderCount".formatted(i)).value(product.getBidderCount()))
                    .andExpect(jsonPath("$.data.content[%d].location".formatted(i)).value(product.getLocation()))
                    .andExpect(jsonPath("$.data.content[%d].thumbnailUrl".formatted(i)).value(product.getThumbnailUrl()))
                    .andExpect(jsonPath("$.data.content[%d].seller.id".formatted(i)).value(product.getSellerId()));
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 키워드 검색")
    void getProductsByElasticsearchByKeyword() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("keyword", "아이폰"))
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto("아이폰", null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 검색된 상품이 키워드를 포함하는지 확인
        List<ProductDocument> products = productPage.getContent();
        for (int i = 0; i < products.size(); i++) {
            resultActions.andExpect(jsonPath("$.data.content[%d].name".formatted(i)).value(Matchers.containsString("아이폰")));
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 카테고리 필터링")
    void getProductsByElasticsearchByCategory() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("category", "1")) // 전자기기
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto(null, new Integer[]{1}, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));
    }

    @Test
    @DisplayName("상품 목록 조회 - 지역 필터링")
    void getProductsByElasticsearchByLocation() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("location", "서울"))
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto(null, null, new String[]{"서울"}, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 검색된 상품이 서울 지역을 포함하는지 확인
        List<ProductDocument> products = productPage.getContent();
        for (int i = 0; i < products.size(); i++) {
            resultActions.andExpect(jsonPath("$.data.content[%d].location".formatted(i)).value(Matchers.containsString("서울")));
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 배송 가능 필터링")
    void getProductsByElasticsearchByDelivery() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("isDelivery", "true"))
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto(null, null, null, true, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));
    }

    @Test
    @DisplayName("상품 목록 조회 - 복합 검색 조건")
    void getProductsByElasticsearchByComplexConditions() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("keyword", "갤럭시")
                        .param("category", "1")
                        .param("isDelivery", "true"))
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto("갤럭시", new Integer[]{1}, null, true, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));
    }

    @Test
    @DisplayName("상품 목록 조회 - 페이징")
    void getProductsByElasticsearchWithPaging() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("page", "1")
                        .param("size", "2"))
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 2, ProductSearchSortType.LATEST,
                new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.data.pageable.totalPages").value(productPage.getTotalPages()))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.pageable.hasNext").value(productPage.hasNext()))
                .andExpect(jsonPath("$.data.pageable.hasPrevious").value(productPage.hasPrevious()))
                .andExpect(jsonPath("$.data.content.length()").value(Math.min(2, productPage.getContent().size())));
    }

    @Test
    @DisplayName("상품 목록 조회 - 가격 오름차순 정렬")
    void getProductsByElasticsearchSortedByPriceAsc() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("sort", "PRICE_LOW"))
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.PRICE_LOW,
                new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 가격 순서 확인
        List<ProductDocument> products = productPage.getContent();
        for (int i = 0; i < products.size() - 1; i++) {
            Long currentPrice = products.get(i).getCurrentPrice();
            Long nextPrice = products.get(i + 1).getCurrentPrice();
            // 현재 가격이 다음 가격보다 작거나 같아야 함
            assert currentPrice <= nextPrice : "Price ordering is incorrect";
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 인기순 정렬")
    void getProductsByElasticsearchSortedByPopularity() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("sort", "POPULAR"))
                .andDo(print());

        Page<ProductDocument> productPage = productSearchService.searchProducts(1, 20, ProductSearchSortType.POPULAR,
                new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 아이폰이 입찰자가 많아서 첫 번째에 와야 함
        if (!productPage.getContent().isEmpty()) {
            resultActions.andExpect(jsonPath("$.data.content[0].name").value(Matchers.containsString("아이폰")));
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 검색 결과 없음")
    void getProductsByElasticsearchEmptyResult() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es")
                        .param("keyword", "존재하지않는상품"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("내 상품 목록 조회 - Elasticsearch")
    @WithMockUser("user1@example.com")
    void getMyProductsByElasticsearch() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es/me"))
                .andDo(print());

        Member actor = memberRepository.findByEmail("user1@example.com").get();
        Page<ProductDocument> productPage = productSearchService.searchProductsByMember(
                1, 20, ProductSearchSortType.LATEST, actor, SaleStatus.SELLING
        );

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("getMyProductsByElasticsearch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("내 상품 목록이 조회되었습니다"))
                .andExpect(jsonPath("$.data.pageable.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.data.pageable.totalPages").value(productPage.getTotalPages()))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.pageable.hasNext").value(productPage.hasNext()))
                .andExpect(jsonPath("$.data.pageable.hasPrevious").value(productPage.hasPrevious()));

        List<ProductDocument> products = productPage.getContent();
        resultActions.andExpect(jsonPath("$.data.content.length()").value(products.size()));

        for (int i = 0; i < products.size(); i++) {
            ProductDocument product = products.get(i);
            resultActions
                    .andExpect(jsonPath("$.data.content[%d].productId".formatted(i)).value(product.getProductId()))
                    .andExpect(jsonPath("$.data.content[%d].name".formatted(i)).value(product.getProductName()))
                    .andExpect(jsonPath("$.data.content[%d].status".formatted(i)).value(product.getStatus()));
        }
    }

    @Test
    @DisplayName("내 상품 목록 조회 - 판매 완료 필터링 - Elasticsearch")
    @WithMockUser("user1@example.com")
    void getMyProductsByElasticsearchWithSoldStatus() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es/me")
                        .param("status", "SOLD"))
                .andDo(print());

        Member actor = memberRepository.findByEmail("user1@example.com").get();
        Page<ProductDocument> productPage = productSearchService.searchProductsByMember(
                1, 20, ProductSearchSortType.LATEST, actor, SaleStatus.SOLD
        );

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("getMyProductsByElasticsearch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("내 상품 목록이 조회되었습니다"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()));
    }

    @Test
    @DisplayName("내 상품 목록 조회 - 인기순 정렬 - Elasticsearch")
    @WithMockUser("user1@example.com")
    void getMyProductsByElasticsearchSortedByPopularity() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es/me")
                        .param("sort", "POPULAR"))
                .andDo(print());

        Member actor = memberRepository.findByEmail("user1@example.com").get();
        Page<ProductDocument> productPage = productSearchService.searchProductsByMember(
                1, 20, ProductSearchSortType.POPULAR, actor, SaleStatus.SELLING
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 아이폰이 입찰자가 많아서 첫 번째에 와야 함
        if (!productPage.getContent().isEmpty()) {
            resultActions.andExpect(jsonPath("$.data.content[0].name").value(Matchers.containsString("아이폰")));
        }
    }

    @Test
    @DisplayName("특정 회원 상품 목록 조회 - Elasticsearch")
    void getProductsByMemberAndElasticsearch() throws Exception {
        // when
        Long memberId = 1L;
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es/members/" + memberId))
                .andDo(print());

        Member actor = memberRepository.findById(memberId).get();
        Page<ProductDocument> productPage = productSearchService.searchProductsByMember(
                1, 20, ProductSearchSortType.LATEST, actor, SaleStatus.SELLING
        );

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("getProductsByMemberAndElasticsearch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("%d번 회원 상품 목록이 조회되었습니다".formatted(memberId)))
                .andExpect(jsonPath("$.data.pageable.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.data.pageable.totalPages").value(productPage.getTotalPages()))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()));

        List<ProductDocument> products = productPage.getContent();
        resultActions.andExpect(jsonPath("$.data.content.length()").value(products.size()));
    }

    @Test
    @DisplayName("특정 회원 상품 목록 조회 - 판매 완료 필터링 - Elasticsearch")
    void getProductsByMemberAndElasticsearchWithSoldStatus() throws Exception {
        // when
        Long memberId = 1L;
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es/members/" + memberId)
                        .param("status", "SOLD"))
                .andDo(print());

        Member actor = memberRepository.findById(memberId).get();
        Page<ProductDocument> productPage = productSearchService.searchProductsByMember(
                1, 20, ProductSearchSortType.LATEST, actor, SaleStatus.SOLD
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()));
    }

    @Test
    @DisplayName("특정 회원 상품 목록 조회 - 인기순 정렬 - Elasticsearch")
    void getProductsByMemberAndElasticsearchSortedByPopularity() throws Exception {
        // when
        Long memberId = 1L;
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/es/members/" + memberId)
                        .param("sort", "POPULAR"))
                .andDo(print());

        Member actor = memberRepository.findById(memberId).get();
        Page<ProductDocument> productPage = productSearchService.searchProductsByMember(
                1, 20, ProductSearchSortType.POPULAR, actor, SaleStatus.SELLING
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        if (!productPage.getContent().isEmpty()) {
            resultActions.andExpect(jsonPath("$.data.content[0].name").value(Matchers.containsString("아이폰")));
        }
    }
}