package com.backend.fastapi.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 예시용 트리거 발행 클래스
 * 실제로는 FastAPI 서버에서 이 로직을 구현하여 트리거를 발행합니다.
 * 
 * 이 클래스는 테스트나 예시 목적으로만 사용됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiTriggerPublisher {
    
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Redis Pub/Sub 토픽 이름 (리스너와 동일)
     */
    private static final String TRIGGER_TOPIC = "fastapi:trigger";
    
    /**
     * 예시: 트리거 발행
     * 
     * 실제 FastAPI 서버에서는 다음과 같은 방식으로 트리거를 발행합니다:
     * 
     * Python 예시:
     * ```python
     * import redis
     * import json
     * 
     * r = redis.Redis(host='localhost', port=6379)
     * 
     * trigger_data = {
     *     "triggerType": "data_received",
     *     "triggerId": str(uuid.uuid4()),
     *     "timestamp": int(time.time() * 1000),
     *     "data": {
     *         "userId": "12345",
     *         "action": "process_data"
     *     },
     *     "apiEndpoints": [
     *         "/api/v1/process",
     *         "/api/v1/validate",
     *         "/api/v1/notify"
     *     ]
     * }
     * 
     * r.publish("fastapi:trigger", json.dumps(trigger_data))
     * ```
     * 
     * @param triggerType 트리거 타입
     * @param data 추가 데이터
     * @param apiEndpoints 실행할 API 엔드포인트 목록
     */
    public void publishTrigger(String triggerType, Map<String, Object> data, String... apiEndpoints) {
        try {
            RTopic topic = redissonClient.getTopic(TRIGGER_TOPIC);
            
            // 트리거 데이터 구성
            Map<String, Object> triggerData = new HashMap<>();
            triggerData.put("triggerType", triggerType);
            triggerData.put("triggerId", UUID.randomUUID().toString());
            triggerData.put("timestamp", System.currentTimeMillis());
            triggerData.put("data", data != null ? data : new HashMap<>());
            triggerData.put("apiEndpoints", apiEndpoints);
            
            // JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(triggerData);
            
            // Redis Pub/Sub으로 발행
            long subscribers = topic.publish(message);
            
            log.info("[FastAPI Trigger Publisher] 트리거 발행 완료 - triggerType: {}, triggerId: {}, subscribers: {}", 
                    triggerType, triggerData.get("triggerId"), subscribers);
            
        } catch (Exception e) {
            log.error("[FastAPI Trigger Publisher] 트리거 발행 실패 - triggerType: {}", triggerType, e);
        }
    }
    
    /**
     * 예시: 데이터 수신 트리거 발행
     */
    public void publishDataReceivedTrigger(String userId, String dataId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("dataId", dataId);
        data.put("action", "data_received");
        
        publishTrigger(
                "data_received",
                data,
                "/api/v1/process",
                "/api/v1/validate",
                "/api/v1/notify"
        );
    }
    
    /**
     * 예시: 처리 완료 트리거 발행
     */
    public void publishProcessingCompleteTrigger(String taskId, String result) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("result", result);
        data.put("action", "processing_complete");
        
        publishTrigger(
                "processing_complete",
                data,
                "/api/v1/update-status",
                "/api/v1/send-notification"
        );
    }
}

