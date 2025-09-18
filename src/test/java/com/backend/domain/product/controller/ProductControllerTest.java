package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.service.FileService;
import com.backend.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private FileService fileService;

    @Test
    @DisplayName("상품 생성")
    void createProduct() throws Exception {
        // Given
        String requestJson = """
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
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", requestJson.getBytes()
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

        // FileService Mock 설정 (파일 업로드 시뮬레이션)
        when(fileService.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("http://localhost:8080/uploads/products/1/test.jpg");

        // When & Then
        mvc.perform(multipart("/products")
                        .file(requestPart)
                        .file(imagePart1)
                        .file(imagePart2))
                .andDo(print())
                .andExpect(handler().handlerType(ProductController.class))
                .andExpect(handler().methodName("createProduct"))
                .andExpect(status().isCreated())  // Controller가 실제 반환하는 상태 코드에 맞게
                .andExpect(jsonPath("$.resultCode").value("201-1"))  // 실제 값에 맞게
                .andExpect(jsonPath("$.msg").value("상품이 등록되었습니다."))
                .andExpect(jsonPath("$.data.name").value("아이폰 15 Pro 256GB"));

        verify(productService).createProduct(
                any(Member.class),
                any(ProductCreateRequest.class),
                argThat(images -> images.size() == 2)
        );
    }
}