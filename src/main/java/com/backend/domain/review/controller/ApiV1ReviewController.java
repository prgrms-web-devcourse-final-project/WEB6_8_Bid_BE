package com.backend.domain.review.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.review.dto.ReviewRequest;
import com.backend.domain.review.dto.ReviewResponse;
import com.backend.domain.review.service.ReviewService;
import com.backend.global.response.RsData;
import com.backend.global.response.RsStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ApiV1ReviewController {

    private final ReviewService reviewService;
    private final MemberRepository memberRepository;

    private Member getMember(User user) {
        return memberRepository.findByEmail(user.getUsername()).orElseThrow(() -> new RuntimeException("Member not found"));
    }

    @PostMapping
    public ResponseEntity<RsData<ReviewResponse>> createReview(@AuthenticationPrincipal User user, @RequestBody ReviewRequest request) {
        Member member = getMember(user);
        ReviewResponse response = reviewService.createReview(member.getId(), request);
        return ResponseEntity.ok(RsData.created("리뷰가 성공적으로 등록되었습니다.", response));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<RsData<ReviewResponse>> getReview(@PathVariable Long reviewId) {
        ReviewResponse response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(RsData.ok("리뷰를 성공적으로 조회했습니다.", response));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<RsData<List<ReviewResponse>>> getReviewsByProductId(@PathVariable Long productId) {
        List<ReviewResponse> responses = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(RsData.ok("리뷰를 성공적으로 조회했습니다.", responses));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<RsData<ReviewResponse>> updateReview(@AuthenticationPrincipal User user, @PathVariable Long reviewId, @RequestBody ReviewRequest request) {
        Member member = getMember(user);
        ReviewResponse response = reviewService.updateReview(member.getId(), reviewId, request);
        return ResponseEntity.ok(RsData.ok("리뷰가 성공적으로 수정되었습니다.", response));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<RsData<Void>> deleteReview(@AuthenticationPrincipal User user, @PathVariable Long reviewId) {
        Member member = getMember(user);
        reviewService.deleteReview(member.getId(), reviewId);
        return ResponseEntity.ok(RsData.ok("리뷰가 성공적으로 삭제되었습니다."));
    }
}