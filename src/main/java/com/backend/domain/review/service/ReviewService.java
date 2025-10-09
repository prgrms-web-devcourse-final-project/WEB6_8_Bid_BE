package com.backend.domain.review.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import com.backend.domain.review.dto.ReviewRequest;
import com.backend.domain.review.dto.ReviewResponse;
import com.backend.domain.review.entity.Review;
import com.backend.domain.review.exception.ReviewException;
import com.backend.domain.review.repository.ReviewRepository;
import com.backend.global.response.RsStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public ReviewResponse createReview(Long memberId, ReviewRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ReviewException(RsStatus.MEMBER_NOT_FOUND));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ReviewException(RsStatus.PRODUCT_NOT_FOUND));

        reviewRepository.findByProductId(product.getId()).ifPresent(review -> {
            throw ReviewException.alreadyExists();
        });

        Review review = Review.builder()
                .reviewer(member)
                .product(product)
                .comment(request.comment())
                .isSatisfied(request.isSatisfied())
                .build();

        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.from(savedReview);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewException::reviewNotFound);
        return ReviewResponse.from(review);
    }

    public ReviewResponse updateReview(Long memberId, Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewException::reviewNotFound);

        if (!review.getReviewer().getId().equals(memberId)) {
            throw ReviewException.accessDenied();
        }

        review.update(request.comment(), request.isSatisfied());
        return ReviewResponse.from(review);
    }

    public void deleteReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewException::reviewNotFound);

        if (!review.getReviewer().getId().equals(memberId)) {
            throw ReviewException.accessDenied();
        }

        reviewRepository.delete(review);
    }
}