package com.backend.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDto(
        @Schema(description = "Access Token")
        String accessToken,
        @Schema(description = "Refresh Token")
        String refreshToken
) {
}
