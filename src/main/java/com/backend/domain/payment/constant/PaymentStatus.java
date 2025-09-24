package com.backend.domain.payment.constant;

public enum PaymentStatus {
    PENDING,   // 아직 처리 중..
    SUCCESS,   // 성공..
    FAILED,    // 실패..
    CANCELED   // 사용자가 취소..
}