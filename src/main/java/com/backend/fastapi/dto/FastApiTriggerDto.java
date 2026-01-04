package com.backend.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * FastAPI 서버로부터 받는 트리거 데이터 DTO
 * Redis Pub/Sub을 통해 전달되는 메시지의 구조
 */
@Getter
@ToString
public class FastApiTriggerDto {
    
    /**
     * 트리거 타입 (예: "data_received", "processing_complete", "webhook_event" 등)
     */
    private final String triggerType;
    
    /**
     * 트리거 ID (고유 식별자)
     */
    private final String triggerId;
    
    /**
     * 트리거가 발생한 시간 (Unix timestamp)
     */
    private final Long timestamp;
    
    /**
     * 추가 데이터 (트리거 타입에 따라 다른 구조를 가질 수 있음)
     */
    private final Map<String, Object> data;
    
    /**
     * 실행할 API 엔드포인트 목록 (순차적으로 실행됨)
     */
    private final String[] apiEndpoints;
    
    @JsonCreator
    public FastApiTriggerDto(
            @JsonProperty("triggerType") String triggerType,
            @JsonProperty("triggerId") String triggerId,
            @JsonProperty("timestamp") Long timestamp,
            @JsonProperty("data") Map<String, Object> data,
            @JsonProperty("apiEndpoints") String[] apiEndpoints) {
        this.triggerType = triggerType;
        this.triggerId = triggerId;
        this.timestamp = timestamp != null ? timestamp : System.currentTimeMillis();
        this.data = data;
        this.apiEndpoints = apiEndpoints;
    }
}

