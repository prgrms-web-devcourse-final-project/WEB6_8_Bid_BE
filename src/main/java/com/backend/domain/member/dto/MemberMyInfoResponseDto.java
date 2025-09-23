package com.backend.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberMyInfoResponseDto(
        @Schema(description = "결과 코드", example = "200")
        String resultCode,

        @Schema(description = "응답 메시지", example = "로그아웃 되었습니다.")
        String msg
){

}
