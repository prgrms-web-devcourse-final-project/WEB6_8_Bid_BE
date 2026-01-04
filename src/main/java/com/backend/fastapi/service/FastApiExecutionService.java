package com.backend.fastapi.service;

import com.backend.fastapi.dto.FastApiTriggerDto;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * FastAPI 트리거를 받아 순차적으로 API를 실행하는 서비스
 * 단일 스레드 ExecutorService를 사용하여 순차 실행 보장
 * 프로덕션 환경을 위한 리소스 관리 및 에러 처리 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiExecutionService {
    
    @Value("${app.fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;
    
    @Value("${app.fastapi.timeout.connect:10}")
    private int connectTimeoutSeconds;
    
    @Value("${app.fastapi.timeout.read:30}")
    private int readTimeoutSeconds;
    
    @Value("${app.fastapi.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    // 순차 실행을 위한 단일 스레드 ExecutorService
    // 프로덕션 환경을 위해 명시적으로 관리
    private final ExecutorService sequentialExecutor = Executors.newSingleThreadExecutor(
            r -> {
                Thread t = new Thread(r, "fastapi-sequential-executor");
                t.setDaemon(false); // 프로덕션에서는 데몬 스레드 비활성화
                return t;
            }
    );
    
    // WebClient는 프로덕션 환경을 위한 타임아웃 설정 포함
    private final WebClient webClient = createWebClient();
    
    private WebClient createWebClient() {
        // 프로덕션 환경을 위한 HttpClient 설정
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .option(reactor.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                        connectTimeoutSeconds * 1000);
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
    
    /**
     * 트리거를 받아 순차적으로 API를 실행
     * 
     * @param trigger 트리거 데이터
     */
    public void executeApisSequentially(FastApiTriggerDto trigger) {
        log.info("[FastAPI Trigger] 트리거 수신 - triggerId: {}, triggerType: {}, apiCount: {}", 
                trigger.getTriggerId(), trigger.getTriggerType(), 
                trigger.getApiEndpoints() != null ? trigger.getApiEndpoints().length : 0);
        
        // 비동기로 실행하되, 순차 실행을 보장하기 위해 단일 스레드 ExecutorService 사용
        CompletableFuture.runAsync(() -> {
            try {
                if (trigger.getApiEndpoints() == null || trigger.getApiEndpoints().length == 0) {
                    log.warn("[FastAPI Trigger] 실행할 API 엔드포인트가 없습니다. triggerId: {}", trigger.getTriggerId());
                    return;
                }
                
                // 순차적으로 각 API 실행
                for (int i = 0; i < trigger.getApiEndpoints().length; i++) {
                    String endpoint = trigger.getApiEndpoints()[i];
                    executeApi(trigger, endpoint, i + 1, trigger.getApiEndpoints().length);
                }
                
                log.info("[FastAPI Trigger] 모든 API 실행 완료 - triggerId: {}", trigger.getTriggerId());
                
            } catch (Exception e) {
                log.error("[FastAPI Trigger] API 실행 중 오류 발생 - triggerId: {}", trigger.getTriggerId(), e);
            }
        }, sequentialExecutor);
    }
    
    /**
     * 단일 API 엔드포인트 실행
     * 프로덕션 환경을 위한 타임아웃 및 재시도 로직 포함
     * 
     * @param trigger 트리거 데이터
     * @param endpoint 실행할 API 엔드포인트 (쿼리 파라미터 포함 가능, 예: "/api/v1/users?userId=123")
     * @param currentIndex 현재 실행 중인 API 인덱스
     * @param totalCount 전체 API 개수
     */
    private void executeApi(FastApiTriggerDto trigger, String endpoint, int currentIndex, int totalCount) {
        try {
            log.info("[FastAPI Execution] API 실행 시작 [{}/{}] - endpoint: {}, triggerId: {}", 
                    currentIndex, totalCount, endpoint, trigger.getTriggerId());
            
            // 요청 본문 구성 (트리거 데이터 포함)
            // data 필드에 있는 모든 데이터가 requestBody에 포함됨
            Map<String, Object> requestBody = Map.of(
                    "triggerId", trigger.getTriggerId(),
                    "triggerType", trigger.getTriggerType(),
                    "timestamp", trigger.getTimestamp(),
                    "data", trigger.getData() != null ? trigger.getData() : Map.of()
            );
            
            // API 호출 (동기적으로 실행하여 순차 보장)
            // 프로덕션 환경을 위한 타임아웃 및 재시도 설정
            // endpoint에 쿼리 파라미터가 포함되어 있으면 그대로 사용됨
            Map<String, Object> response = webClient.post()
                    .uri(fastApiBaseUrl + endpoint)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        log.error("[FastAPI Execution] API 호출 실패 - endpoint: {}, status: {}, triggerId: {}", 
                                endpoint, clientResponse.statusCode(), trigger.getTriggerId());
                        return Mono.error(new RuntimeException(
                                "API 호출 실패: " + endpoint + " - " + clientResponse.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(readTimeoutSeconds)) // 읽기 타임아웃
                    .retryWhen(Retry.fixedDelay(maxRetryAttempts, Duration.ofSeconds(2))
                            .filter(throwable -> {
                                // 네트워크 오류나 타임아웃만 재시도
                                return throwable instanceof java.util.concurrent.TimeoutException
                                        || (throwable.getCause() instanceof java.net.ConnectException)
                                        || (throwable.getCause() instanceof java.net.SocketTimeoutException);
                            })
                            .doBeforeRetry(retrySignal -> 
                                    log.warn("[FastAPI Execution] 재시도 [{}/{}] - endpoint: {}, triggerId: {}", 
                                            retrySignal.totalRetries() + 1, maxRetryAttempts, 
                                            endpoint, trigger.getTriggerId())))
                    .block(); // 동기 실행으로 순차 보장
            
            log.info("[FastAPI Execution] API 실행 완료 [{}/{}] - endpoint: {}, response: {}, triggerId: {}", 
                    currentIndex, totalCount, endpoint, response, trigger.getTriggerId());
            
        } catch (Exception e) {
            log.error("[FastAPI Execution] API 실행 중 예외 발생 [{}/{}] - endpoint: {}, triggerId: {}", 
                    currentIndex, totalCount, endpoint, trigger.getTriggerId(), e);
            // 예외가 발생해도 다음 API는 계속 실행
            // 필요시 여기서 예외를 다시 던져서 전체 프로세스를 중단할 수도 있음
        }
    }
    
    /**
     * 서비스 종료 시 ExecutorService 정리
     * 프로덕션 환경에서 안전한 종료를 보장
     */
    @PreDestroy
    public void shutdown() {
        log.info("[FastAPI Execution] ExecutorService 종료 중...");
        sequentialExecutor.shutdown();
        try {
            // 최대 30초 대기 후 강제 종료
            if (!sequentialExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("[FastAPI Execution] ExecutorService가 30초 내에 종료되지 않아 강제 종료합니다.");
                sequentialExecutor.shutdownNow();
                // 추가로 10초 대기
                if (!sequentialExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("[FastAPI Execution] ExecutorService가 강제 종료되지 않았습니다.");
                }
            }
            log.info("[FastAPI Execution] ExecutorService 종료 완료");
        } catch (InterruptedException e) {
            log.error("[FastAPI Execution] ExecutorService 종료 중 인터럽트 발생", e);
            sequentialExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

