package com.backend.domain.product.controller;

import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductSearchSortType;
import com.backend.domain.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiV1ProductControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("상품 생성")
    @Transactional
    void createProduct_IntegrationTest_Success() throws Exception {
        // given
        ProductCreateRequest request = createValidRequest();

        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile image1 = new MockMultipartFile("images", "test1.jpg", "image/jpeg", "image1 content".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "test2.png", "image/png", "image2 content".getBytes());

        // when
        ResultActions resultActions = mvc
                .perform(multipart("/api/v1/products")
                        .file(requestPart)
                        .file(image1)
                        .file(image2)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print());

        Product product = productService.findLatest().get();

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value("상품이 등록되었습니다."))
                .andExpect(jsonPath("$.data.name").value(product.getProductName()))
                .andExpect(jsonPath("$.data.description").value(product.getDescription()))
                .andExpect(jsonPath("$.data.category").value(product.getCategory().getDisplayName()))
                .andExpect(jsonPath("$.data.initialPrice").value(product.getInitialPrice()))
                .andExpect(jsonPath("$.data.currentPrice").value(product.getCurrentPrice()))
                .andExpect(jsonPath("$.data.auctionStartTime").value(Matchers.startsWith(product.getStartTime().toString().substring(0, 15))))
                .andExpect(jsonPath("$.data.auctionEndTime").value(Matchers.startsWith(product.getEndTime().toString().substring(0, 15))))
                .andExpect(jsonPath("$.data.auctionDuration").value(product.getDuration()))
                .andExpect(jsonPath("$.data.status").value(product.getStatus()))
//                .andExpect(jsonPath("$.data.biddersCount").value(product.getBiddersCount()))
                .andExpect(jsonPath("$.data.deliveryMethod").value(product.getDeliveryMethod().name()))
                .andExpect(jsonPath("$.data.location").value(product.getLocation()))
                .andExpect(jsonPath("$.data.images.length()").value(product.getProductImages().size()))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value(product.getProductImages().get(0).getImageUrl()))
                .andExpect(jsonPath("$.data.images[1].imageUrl").value(product.getProductImages().get(1).getImageUrl()))
                .andExpect(jsonPath("$.data.seller.id").value(product.getSeller().getId()))
                .andExpect(jsonPath("$.data.createDate").value(Matchers.startsWith(product.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.modifyDate").value(Matchers.startsWith(product.getModifyDate().toString().substring(0, 20))));
    }

    @Test
    @DisplayName("상품 생성 실패 - 이미지 검증 실패 (실제 서비스 로직)")
    void createProduct_FailByImageValidation() throws Exception {
        // given
        ProductCreateRequest request = createValidRequest();

        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile[] images = new MockMultipartFile[6]; // 6개 이미지 (5개 초과로 실패해야 함)
        for (int i = 0; i < 6; i++) {
            images[i] = new MockMultipartFile("images", "test" + i + ".jpg", "image/jpeg", "image content".getBytes());
        }

        // when
        var requestBuilder = multipart("/api/v1/products").file(requestPart);
        for (MockMultipartFile image : images) requestBuilder.file(image);
        ResultActions resultActions = mvc.perform(requestBuilder).andDo(print());

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"))
                .andExpect(jsonPath("$.msg").value("이미지는 최대 5개까지만 업로드할 수 있습니다."));
    }

    @Test
    @DisplayName("상품 생성 실패 - 큰 파일 크기 (실제 파일 검증)")
    void createProduct_FailByLargeFileSize() throws Exception {
        // given
        ProductCreateRequest request = createValidRequest();

        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsBytes(request));
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB 파일 (5MB 초과로 실패해야 함)
        MockMultipartFile largeImage = new MockMultipartFile("images", "large.jpg", "image/jpeg", largeContent);

        // when
        ResultActions resultActions = mvc
                .perform(multipart("/api/v1/products")
                        .file(requestPart)
                        .file(largeImage))
                .andDo(print());

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-5"))
                .andExpect(jsonPath("$.msg").value("이미지 파일 크기는 5MB를 초과할 수 없습니다."));
    }

    @Test
    @DisplayName("상품 생성 실패 - 지원하지 않는 파일 형식")
    void createProduct_FailByUnsupportedFileType() throws Exception {
        // given
        ProductCreateRequest request = createValidRequest();

        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsBytes(request));
        // PDF 파일 업로드 (이미지가 아니므로 실패해야 함)
        MockMultipartFile pdfFile = new MockMultipartFile("images", "document.pdf", "application/pdf", "pdf content".getBytes());

        // when
        ResultActions resultActions = mvc
                .perform(multipart("/api/v1/products")
                        .file(requestPart)
                        .file(pdfFile))
                .andDo(print());

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-7"))
                .andExpect(jsonPath("$.msg").value("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)"));
    }


    @Test
    @DisplayName("상품 목록 조회")
    void getProducts() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/products")
                ).andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("getProducts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("상품 목록이 조회되었습니다."))
                .andExpect(jsonPath("$.data.pageable.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.data.pageable.totalPages").value(productPage.getTotalPages()))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.pageable.hasNext").value(productPage.hasNext()))
                .andExpect(jsonPath("$.data.pageable.hasPrevious").value(productPage.hasPrevious()));

        List<Product> products = productPage.getContent();
        resultActions.andExpect(jsonPath("$.data.content.length()").value(products.size()));

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            resultActions
                    .andExpect(jsonPath("$.data.content[%d].productId".formatted(i)).value(product.getId()))
                    .andExpect(jsonPath("$.data.content[%d].name".formatted(i)).value(product.getProductName()))
                    .andExpect(jsonPath("$.data.content[%d].category".formatted(i)).value(product.getCategory().getDisplayName()))
                    .andExpect(jsonPath("$.data.content[%d].initialPrice".formatted(i)).value(product.getInitialPrice()))
                    .andExpect(jsonPath("$.data.content[%d].currentPrice".formatted(i)).value(product.getCurrentPrice()))
                    .andExpect(jsonPath("$.data.content[%d].auctionStartTime".formatted(i)).value(Matchers.startsWith(product.getStartTime().toString().substring(0, 15))))
                    .andExpect(jsonPath("$.data.content[%d].auctionEndTime".formatted(i)).value(Matchers.startsWith(product.getEndTime().toString().substring(0, 15))))
                    .andExpect(jsonPath("$.data.content[%d].auctionDuration".formatted(i)).value(product.getDuration()))
                    .andExpect(jsonPath("$.data.content[%d].status".formatted(i)).value(product.getStatus()))
//                    .andExpect(jsonPath("$.data.content[%d].biddersCount".formatted(i)).value(product.getBiddersCount()))
                    .andExpect(jsonPath("$.data.content[%d].location".formatted(i)).value(product.getLocation()))
                    .andExpect(jsonPath("$.data.content[%d].thumbnailUrl".formatted(i)).value(product.getThumbnail()))
                    .andExpect(jsonPath("$.data.content[%d].seller.id".formatted(i)).value(product.getSeller().getId()));
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 키워드 검색")
    void getProductsByKeyword() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("keyword", "아이폰"))
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto("아이폰", null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 검색된 상품이 키워드를 포함하는지 확인
        List<Product> products = productPage.getContent();
        for (int i = 0; i < products.size(); i++) {
            resultActions.andExpect(jsonPath("$.data.content[%d].name".formatted(i)).value(Matchers.containsString("아이폰")));
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 카테고리 필터링")
    void getProductsByCategory() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("category", "1")) // 전자기기
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.LATEST,
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
    void getProductsByLocation() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("location", "서울"))
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.LATEST,
                new ProductSearchDto(null, null, new String[]{"서울"}, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(productPage.getTotalElements()))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 검색된 상품이 서울 지역을 포함하는지 확인
        List<Product> products = productPage.getContent();
        for (int i = 0; i < products.size(); i++) {
            resultActions.andExpect(jsonPath("$.data.content[%d].location".formatted(i)).value(Matchers.containsString("서울")));
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 배송 가능 필터링")
    void getProductsByDelivery() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("isDelivery", "true"))
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.LATEST,
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
    void getProductsByComplexConditions() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("keyword", "갤럭시")
                        .param("category", "1")
                        .param("isDelivery", "true"))
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.LATEST,
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
    void getProductsWithPaging() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("page", "1")
                        .param("size", "2"))
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 2, ProductSearchSortType.LATEST,
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
    void getProductsSortedByPriceAsc() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("sort", "PRICE_LOW"))
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.PRICE_LOW,
                new ProductSearchDto(null, null, null, null, AuctionStatus.BIDDING));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.content.length()").value(productPage.getContent().size()));

        // 가격 순서 확인
        List<Product> products = productPage.getContent();
        for (int i = 0; i < products.size() - 1; i++) {
            Long currentPrice = products.get(i).getCurrentPrice();
            Long nextPrice = products.get(i + 1).getCurrentPrice();
            // 현재 가격이 다음 가격보다 작거나 같아야 함
            assert currentPrice <= nextPrice : "Price ordering is incorrect";
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 인기순 정렬")
    void getProductsSortedByPopularity() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
                        .param("sort", "POPULAR"))
                .andDo(print());

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.POPULAR,
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
    void getProductsEmptyResult() throws Exception {
        // when
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products")
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
    @DisplayName("상품 상세 조회")
    @Transactional
    void getProduct() throws Exception {
        // when
        Long id = 1L;
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/" + id))
                .andDo(print());

        Product product = productService.findById(id).get();

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("getProduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("상품이 조회되었습니다."))
                .andExpect(jsonPath("$.data.name").value(product.getProductName()))
                .andExpect(jsonPath("$.data.description").value(product.getDescription()))
                .andExpect(jsonPath("$.data.category").value(product.getCategory().getDisplayName()))
                .andExpect(jsonPath("$.data.initialPrice").value(product.getInitialPrice()))
                .andExpect(jsonPath("$.data.currentPrice").value(product.getCurrentPrice()))
                .andExpect(jsonPath("$.data.auctionStartTime").value(Matchers.startsWith(product.getStartTime().toString().substring(0, 15))))
                .andExpect(jsonPath("$.data.auctionEndTime").value(Matchers.startsWith(product.getEndTime().toString().substring(0, 15))))
                .andExpect(jsonPath("$.data.auctionDuration").value(product.getDuration()))
                .andExpect(jsonPath("$.data.status").value(product.getStatus()))
//                .andExpect(jsonPath("$.data.biddersCount").value(product.getBiddersCount()))
                .andExpect(jsonPath("$.data.deliveryMethod").value(product.getDeliveryMethod().name()))
                .andExpect(jsonPath("$.data.location").value(product.getLocation()))
                .andExpect(jsonPath("$.data.images.length()").value(product.getProductImages().size()))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value(product.getProductImages().get(0).getImageUrl()))
                .andExpect(jsonPath("$.data.seller.id").value(product.getSeller().getId()))
                .andExpect(jsonPath("$.data.createDate").value(Matchers.startsWith(product.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.modifyDate").value(Matchers.startsWith(product.getModifyDate().toString().substring(0, 20))));
    }

    @Test
    @DisplayName("상품 상세 조회 실패")
    void getProduct_failed() throws Exception {
        // when
        long id = Long.MAX_VALUE;
        ResultActions resultActions = mvc
                .perform(get("/api/v1/products/" + id))
                .andDo(print());

        // then
        resultActions
                .andExpect(handler().handlerType(ApiV1ProductController.class))
                .andExpect(handler().methodName("getProduct"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 상품입니다."));
    }


    // ======================================= Helper methods ======================================= //
    private ProductCreateRequest createValidRequest() {
        return new ProductCreateRequest(
                "테스트 상품",
                "상품 설명",
                1,
                1000L,
                LocalDateTime.now().plusDays(1),
                "24시간",
                DeliveryMethod.DELIVERY,
                "서울특별시"
        );
    }
}