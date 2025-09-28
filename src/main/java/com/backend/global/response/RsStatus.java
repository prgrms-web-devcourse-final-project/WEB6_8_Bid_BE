package com.backend.global.response;

import lombok.Getter;

@Getter
public enum RsStatus {
    // 성공 관련
    OK(200, "성공"),
    CREATED(201, "생성 완료"),
    ACCEPTED(202, "요청 접수"),
    
    // 클라이언트 오류
    BAD_REQUEST(400, "잘못된 요청"),
    UNAUTHORIZED(401, "인증 실패"),
    FORBIDDEN(403, "권한 없음"),
    NOT_FOUND(404, "리소스를 찾을 수 없음"),
    CONFLICT(409, "중복된 요청"),
    
    // 서버 오류
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류"),
    SERVICE_UNAVAILABLE(503, "서비스 이용 불가");
    
    private final int statusCode;
    private final String defaultMessage;
    
    RsStatus(int statusCode, String defaultMessage) {
        this.statusCode = statusCode;
        this.defaultMessage = defaultMessage;
    }

    public String getResultCode() {
        return String.valueOf(statusCode);
    }
}