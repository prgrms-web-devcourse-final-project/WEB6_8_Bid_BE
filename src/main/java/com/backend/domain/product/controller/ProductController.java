package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.dto.ProductDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.service.ProductService;
import com.backend.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public RsData<ProductDto> createProduct(
            @RequestPart("request") @Valid ProductCreateRequest request,
            @RequestPart("images") List<MultipartFile> images
    ) {
        // TODO: JWT 토큰에서 사용자 추출
        // Member actor = rq.getActor();
        Member actor = new Member();

        Product product = productService.createProduct(actor, request, images);

        return new RsData<>("201-1", "상품이 등록되었습니다.", ProductDto.fromEntity(product));
    }
}
