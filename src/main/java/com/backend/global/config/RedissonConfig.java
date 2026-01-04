package com.backend.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// 테스트 환경에서는 TestRedisConfiguration 사용
@Configuration
@Profile("!test & !bidtest")
public class RedissonConfig {
    
    @Value("${spring.data.redis.host}")
    private String redisHost;
    
    @Value("${spring.data.redis.port}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    private static final String REDISSON_HOST_PREFIX = "redis://";
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 프로덕션 환경을 위한 설정 (24시간 운영)
        config.useSingleServer()
                .setAddress(REDISSON_HOST_PREFIX + redisHost + ":" + redisPort)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                // 연결 풀 설정
                .setConnectionPoolSize(64)              // 연결 풀 크기
                .setConnectionMinimumIdleSize(32)       // 최소 유휴 연결 수
                // 타임아웃 설정
                .setConnectTimeout(10000)               // 연결 타임아웃 (10초)
                .setTimeout(3000)                       // 명령 타임아웃 (3초)
                .setRetryAttempts(3)                    // 재시도 횟수
                .setRetryInterval(1500)                 // 재시도 간격 (1.5초)
                // 재연결 설정
                .setReconnectionTimeout(3000)          // 재연결 타임아웃 (3초)
                .setKeepAlive(true)                     // Keep-Alive 활성화
                .setTcpNoDelay(true)                    // TCP_NODELAY 활성화
                // 네트워크 모니터링
                .setPingConnectionInterval(30000)       // Ping 간격 (30초)
                .setDnsMonitoringInterval(5000);        // DNS 모니터링 간격 (5초)
        
        return Redisson.create(config);
    }
}
