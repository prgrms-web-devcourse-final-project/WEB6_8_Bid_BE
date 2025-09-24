package com.backend.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MemberModifyRequestDto(
        @Schema(description = "닉네임", example = "example123")
        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        String nickname,

        @Schema(description = "프로필 이미지", example = "example123")
        String profileImageUrl,

        @Schema(description = "휴대폰 번호", example = "010-0000-0000")
        @NotBlank(message = "휴대폰 번호는 필수 입력값입니다.")
        String phone,

        @Schema(description = "주소", example = "서울 강남구...")
        @NotBlank(message = "주소값은 필수 입력값입니다.")
        String address
) {
}
