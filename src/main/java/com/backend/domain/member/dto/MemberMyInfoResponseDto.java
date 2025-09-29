package com.backend.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record MemberMyInfoResponseDto(
        @Schema(description = "DB 아이디", example = "1")
        Long id,

        @Schema(description = "이메일", example = "test@test.com")
        String email,

        @Schema(description = "닉네임", example = "test")
        String nickname,

        @Schema(description = "전화번호", example = "010-0000-0000")
        String phoneNumber,

        @Schema(description = "주소", example = "서울특별시 강남구...")
        String address,

        @Schema(description = "프로필 이미지", example = "https://example.com/profile.jpg")
        String profileImageUrl,

        @Schema(description = "신뢰 포인트", example = "50")
        Integer creditScore,

        @Schema(description = "생성일", example = "2022-01-01T00:00:00")
        LocalDateTime createDate,

        @Schema(description = "업데이트일", example = "2022-01-01T00:00:00")
        LocalDateTime modifyDate
){

}
