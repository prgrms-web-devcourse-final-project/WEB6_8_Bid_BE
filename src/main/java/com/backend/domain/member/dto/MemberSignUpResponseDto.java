package com.backend.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberSignUpResponseDto(
        @Schema(description = "생성된 회원 ID", example = "1")
        Long memberId,
        @Schema(description = "회원 이메일", example = "test@example.com")
        String email,
        @Schema(description = "회원 닉네임", example = "testUser1")
        String nickname
) {
}