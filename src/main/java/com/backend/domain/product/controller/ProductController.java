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

import java.util.Arrays;
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
        validateImages(images);

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

    private void validateImages(List<MultipartFile> images) {
        if (images.size() > 5) {
            throw new ServiceException("400-2", "이미지는 최대 5개까지만 업로드할 수 있습니다.");
        }

        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                // 파일 크기 검증
                if (image.getSize() > 5 * 1024 * 1024) {
                    throw new ServiceException("400-3", "이미지 파일 크기는 5MB를 초과할 수 없습니다.");
                }

                // 파일 확장자 검증
                String filename = image.getOriginalFilename();
                String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
                List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");

                if (!allowedExtensions.contains(extension)) {
                    throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
                }
            }
        }
    }
}
