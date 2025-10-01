package com.backend.domain.member.dto;

import com.backend.domain.member.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberInfoResponseDto {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private int creditScore;

    public MemberInfoResponseDto(Member member) {
        this.id = member.getId();
        this.nickname = member.getNickname();
        this.profileImageUrl = member.getProfileImageUrl();
        this.creditScore = member.getCreditScore();
    }
}