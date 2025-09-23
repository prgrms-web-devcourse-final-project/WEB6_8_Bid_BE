package com.backend.domain.product.controller;

import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.dto.ProductSearchDto;
import com.backend.domain.product.entity.Product;
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
                .andExpect(jsonPath("$.data.category").value(product.getCategory().name()))
                .andExpect(jsonPath("$.data.initialPrice").value(product.getInitialPrice()))
                .andExpect(jsonPath("$.data.currentPrice").value(product.getCurrentPrice()))
                .andExpect(jsonPath("$.data.auctionStartTime").value(product.getStartTime().toString()))
                .andExpect(jsonPath("$.data.auctionEndTime").value(product.getEndTime().toString()))
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

        Page<Product> productPage = productService.findBySearchPaged(1, 20, ProductSearchSortType.LATEST, new ProductSearchDto(null, null, null, null, null));

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
                    .andExpect(jsonPath("$.data.content[%d].category".formatted(i)).value(product.getCategory().name()))
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

    // Helper methods
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