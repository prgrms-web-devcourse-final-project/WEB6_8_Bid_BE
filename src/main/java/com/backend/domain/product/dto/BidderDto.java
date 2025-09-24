package com.backend.domain.product.dto;

import com.backend.domain.member.entity.Member;

public record BidderDto(
        Long id,
        String nickname,
        String profileImageUrl,
        String phoneNumber
) {
    public static BidderDto fromEntity(Member entity) {
        if (entity == null) {
            return null;
        }
        return new BidderDto(
                entity.getId(),
                entity.getNickname(),
                entity.getProfileImageUrl(),
                entity.getPhoneNumber()
        );
    }
}
