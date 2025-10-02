package com.backend.domain.review.controller;

import com.backend.domain.member.dto.MemberMyInfoResponseDto;
import com.backend.domain.review.dto.ReviewEditResponseDto;
import com.backend.domain.review.dto.ReviewResponseDto;
import com.backend.domain.review.dto.ReviewWriteRequestDto;
import com.backend.domain.review.dto.ReviewWriteResponseDto;
import com.backend.domain.review.service.ReviewService;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 관련 API")
public class ApiV1ReviewController {
    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성 API", description = "상품 리뷰 작성")
    @PostMapping("/reviews/products")
    public ResponseEntity<RsData<ReviewWriteResponseDto>> reviewCreate(Authentication authentication, @Valid @RequestPart ReviewWriteRequestDto reviewWriteRequestDto) {
        ReviewWriteResponseDto reviewWriteResponseDto = new ReviewWriteResponseDto(1L, "유저1", "상품1", "좋은 거래였어요!", true);
        return ResponseEntity.ok(new RsData<>("200", "리뷰 작성이 완료되었습니다.", reviewWriteResponseDto));
    }

    @Operation(summary = "리뷰 조회 API", description = "상품 리뷰 조회")
    @GetMapping("/reviews/products/{id}")
    public ResponseEntity<RsData<ReviewResponseDto>> reviewInfo(Authentication authentication, @PathVariable Long id) {
        ReviewResponseDto reviewResponseDto = new ReviewResponseDto(1L, "유저1", 1L,
                "좋은 거래였어요!", true, LocalDateTime.from(Instant.now()), LocalDateTime.from(Instant.now()));
        return ResponseEntity.ok(new RsData<>("200", "리뷰 조회가 완료되었습니다.", reviewResponseDto));
    }

    @Operation(summary = "리뷰 삭제 API", description = "상품 리뷰 삭제")
    @DeleteMapping("/reviews/products")
    public ResponseEntity<RsData<ReviewWriteResponseDto>> reviewDelete(Authentication authentication, @Valid @RequestPart ReviewWriteRequestDto reviewWriteRequestDto) {
        return ResponseEntity.ok(new RsData<>("200", "리뷰 삭제가 완료되었습니다.", null));
    }

    @Operation(summary = "리뷰 수정 API", description = "상품 리뷰 수정")
    @PutMapping("/reviews/products")
    public ResponseEntity<RsData<ReviewEditResponseDto>> reviewModify(Authentication authentication, @Valid @RequestPart ReviewWriteRequestDto reviewWriteRequestDto) {
        ReviewEditResponseDto reviewEditResponseDto = new ReviewEditResponseDto(1L, "유저1", "상품1", "상품이 별로였어요!", false);
        return ResponseEntity.ok(new RsData<>("200", "리뷰 수정이 완료되었습니다.", reviewEditResponseDto));
    }

}
