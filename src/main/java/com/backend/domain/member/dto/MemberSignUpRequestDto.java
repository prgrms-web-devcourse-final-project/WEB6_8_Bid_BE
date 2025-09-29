package com.backend.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberSignUpRequestDto(
        @Schema(description = "회원 이메일", example = "test@example.com")
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "회원 비밀번호", example = "example123")
        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        String password,

        @Schema(description = "회원 닉네임", example = "testUser1")
        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        String nickname,

        @NotBlank(message = "휴대폰 번호는 필수 입력값입니다.")
        String phoneNumber,

        @NotBlank(message = "주소는 필수 입력값입니다.")
        String address
) {
}
