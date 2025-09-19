package com.backend.domain.product.dto;

import com.backend.domain.member.entity.Member;

public record SellerDto(
        Long id,
        String nickname,
//        Integer creditScore,
//        String profileImageUrl,
        Integer reviewCount
) {
    public static SellerDto fromEntity(Member entity) {
        return new SellerDto(
                entity.getId(),
                entity.getNickname(),
//                entity.getCreditScore(),
//                entity.getProfileImageUrl(),
                entity.getReviews().size()
        );
    }
}
