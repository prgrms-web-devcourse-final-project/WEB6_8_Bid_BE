package com.backend.fastapi.listener;

import com.backend.fastapi.dto.FastApiTriggerDto;
import com.backend.fastapi.service.FastApiExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.stereotype.Component;

/**
 * FastAPI 트리거를 Redis Pub/Sub을 통해 수신하는 리스너
 * Redisson의 RTopic을 사용하여 메시지를 구독하고, 수신 시 순차적으로 API를 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiTriggerListener {
    
    private final RedissonClient redissonClient;
    private final FastApiExecutionService fastApiExecutionService;
    private final ObjectMapper objectMapper;
    
    /**
     * Redis Pub/Sub 토픽 이름
     * FastAPI 서버에서 이 토픽으로 메시지를 발행하면 리스너가 수신
     */
    private static final String TRIGGER_TOPIC = "fastapi:trigger";
    
    private RTopic topic;
    private int listenerId;
    
    /**
     * 애플리케이션 시작 시 Redis Pub/Sub 구독 시작
     * 프로덕션 환경을 위한 재시도 로직 포함
     */
    @PostConstruct
    public void subscribe() {
        int maxRetries = 5;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                topic = redissonClient.getTopic(TRIGGER_TOPIC);
                
                // 메시지 리스너 등록
                listenerId = topic.addListener(String.class, new MessageListener<String>() {
                    @Override
                    public void onMessage(CharSequence channel, String message) {
                        handleTriggerMessage(message);
                    }
                });
                
                log.info("[FastAPI Trigger Listener] Redis Pub/Sub 구독 시작 - topic: {}", TRIGGER_TOPIC);
                return; // 성공 시 종료
                
            } catch (Exception e) {
                retryCount++;
                log.error("[FastAPI Trigger Listener] Redis Pub/Sub 구독 실패 (재시도 {}/{}) - topic: {}", 
                        retryCount, maxRetries, TRIGGER_TOPIC, e);
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(3000 * retryCount); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[FastAPI Trigger Listener] 재시도 대기 중 인터럽트 발생", ie);
                        return;
                    }
                } else {
                    log.error("[FastAPI Trigger Listener] Redis Pub/Sub 구독 최종 실패 - topic: {}", TRIGGER_TOPIC);
                }
            }
        }
    }
    
    /**
     * 트리거 메시지 처리
     * 
     * @param message JSON 형식의 트리거 메시지
     */
    private void handleTriggerMessage(String message) {
        try {
            log.debug("[FastAPI Trigger Listener] 트리거 메시지 수신 - message: {}", message);
            
            // JSON 문자열을 FastApiTriggerDto로 변환
            FastApiTriggerDto trigger = objectMapper.readValue(message, FastApiTriggerDto.class);
            
            log.info("[FastAPI Trigger Listener] 트리거 파싱 완료 - triggerId: {}, triggerType: {}", 
                    trigger.getTriggerId(), trigger.getTriggerType());
            
            // 순차적으로 API 실행
            fastApiExecutionService.executeApisSequentially(trigger);
            
        } catch (Exception e) {
            log.error("[FastAPI Trigger Listener] 트리거 메시지 처리 실패 - message: {}", message, e);
        }
    }
    
    /**
     * 애플리케이션 종료 시 리스너 제거
     */
    @PreDestroy
    public void unsubscribe() {
        try {
            if (topic != null && listenerId > 0) {
                topic.removeListener(listenerId);
                log.info("[FastAPI Trigger Listener] Redis Pub/Sub 구독 해제 - topic: {}", TRIGGER_TOPIC);
            }
        } catch (Exception e) {
            log.error("[FastAPI Trigger Listener] Redis Pub/Sub 구독 해제 실패", e);
        }
    }
}

