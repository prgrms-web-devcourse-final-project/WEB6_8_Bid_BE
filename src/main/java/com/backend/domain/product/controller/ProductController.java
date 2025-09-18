package com.backend.domain.product.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.dto.ProductDto;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.service.ProductService;
import com.backend.global.exception.ServiceException;
import com.backend.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public RsData<ProductDto> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @RequestParam("images") @NotBlank List<MultipartFile> images
    ) {
        validateLocation(request.location(), request.deliveryMethod());

        // TODO: JWT 토큰에서 사용자 추출
        // Member actor = rq.getActor();
        Member actor = new Member();

        Product product = productService.createProduct(actor, request, images);

        return new RsData<>("200", "상품이 등록되었습니다.", ProductDto.fromEntity(product));
    }

    private void validateLocation(String location, DeliveryMethod deliveryMethod) {
        if (deliveryMethod == DeliveryMethod.TRADE || deliveryMethod == DeliveryMethod.BOTH) {
            if (location.isBlank()) {
                throw new ServiceException("400-1", "직거래 시 배송지는 필수입니다.");
            }
        }
    }
}
