package com.backend.domain.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TossIssueBillingKeyRequest {
    private String customerKey;
    private String authKey;
}