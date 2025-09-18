package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.repository.ProductImageRepository;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.domain.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Controller 테스트는 로그인 기능 구현된 후에 작성
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성 및 DB에 저장
        Member member = new Member(
                "test@example.com", "password", "testUser", "01000000000", "서울 강남구", "USER", 50, "url",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );
        memberRepository.save(member);
    }

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
                .perform(multipart("/products")
                        .file(requestPart)
                        .file(image1)
                        .file(image2)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print());

        Product product = productService.findLatest().get();

        // then
        resultActions
                .andExpect(handler().handlerType(ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
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
                .andExpect(jsonPath("$.data.biddersCount").value(product.getBiddersCount()))
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
        var requestBuilder = multipart("/products").file(requestPart);
        for (MockMultipartFile image : images) requestBuilder.file(image);
        ResultActions resultActions = mvc.perform(requestBuilder).andDo(print());

        // then
        resultActions
                .andExpect(handler().handlerType(ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
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
        ResultActions resultActions = mvc.perform(multipart("/products")
                        .file(requestPart)
                        .file(largeImage))
                .andDo(print());

        // then
        resultActions
                .andExpect(handler().handlerType(ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
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
        ResultActions resultActions = mvc.perform(multipart("/products")
                        .file(requestPart)
                        .file(pdfFile))
                .andDo(print());

        // then
        resultActions
                .andExpect(handler().handlerType(ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
                .andExpect(jsonPath("$.msg").value("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)"));
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