package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.service.ProductService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("상품 생성")
    void createProduct() throws Exception {
        // Given
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                    "name": "아이폰 15 Pro 256GB",
                    "description": "미개봉 새 제품입니다.",
                    "categoryId": 1,
                    "initialPrice": 1000000,
                    "auctionStartTime": "2024-12-17T09:00:00",
                    "auctionDuration": "24시간",
                    "deliveryMethod": "TRADE",
                    "location": "서울"
                }
                """.getBytes()
        );

        MockMultipartFile imagePart1 = new MockMultipartFile(
                "images",
                "test1.jpg",
                "image/jpeg",
                "test image content 1".getBytes()
        );

        MockMultipartFile imagePart2 = new MockMultipartFile(
                "images",
                "test2.jpg",
                "image/jpeg",
                "test image content 2".getBytes()
        );

        Product mockProduct = createMockProduct();
        when(productService.createProduct(any(Member.class), any(ProductCreateRequest.class), anyList()))
                .thenReturn(mockProduct);

        ResultActions resultActions = mvc
                .perform(
                        multipart("/products")
                                .file(requestPart)
                                .file(imagePart1)
                                .file(imagePart2)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                ).andDo(print());

        resultActions
                .andExpect(handler().handlerType(ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("상품이 등록되었습니다."))
                .andExpect(jsonPath("$.data.name").value("아이폰 15 Pro 256GB"));
    }

    private Product createMockProduct() {
        Member member = new Member();
        return new Product(
                "아이폰 15 Pro 256GB",
                "미개봉 새 제품입니다.",
                ProductCategory.DIGITAL_ELECTRONICS,
                1000000L,
                LocalDateTime.of(2024, 12, 17, 9, 0),
                24,
                DeliveryMethod.TRADE,
                "서울",
                member
        );
    }
}