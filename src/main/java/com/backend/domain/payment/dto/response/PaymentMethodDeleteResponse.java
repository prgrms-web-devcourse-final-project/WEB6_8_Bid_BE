package com.backend.domain.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

// 결제 수단 삭제..
public class PaymentMethodDeleteResponse {
    private final Long id;
    private final boolean deleted;
    private final boolean wasDefault;   // 지운 게 기본이었는지..
    private final Long newDefaultId;    // 승계된 기본 수단 id (없으면 null)..
}