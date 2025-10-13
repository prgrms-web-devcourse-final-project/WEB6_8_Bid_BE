package com.backend.domain.payment.dto.response;

public record IdempotencyKeyResponse(
        String idempotencyKey
) {}