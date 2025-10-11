package com.backend.domain.payment.dto.response;

public record TossBillingAuthParamsResponse(
        String clientKey,
        String customerKey,
        String successUrl,
        String failUrl
) {}