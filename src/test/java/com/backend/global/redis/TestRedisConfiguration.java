package com.backend.global.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

// 테스트용 Embedded Redis 설정 - Lettuce와 Redisson을 모두 지원
@TestConfiguration
public class TestRedisConfiguration implements InitializingBean, DisposableBean {

    private RedisServer redisServer;
    private int redisPort;

    @Override
    public void afterPropertiesSet() throws Exception {
        redisPort = findAvailablePort();
        try {
            redisServer = new RedisServer(redisPort);
            redisServer.start();
            System.out.println("========================================");
            System.out.println("✓ Embedded Redis started on port: " + redisPort);
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("⚠ Redis start failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start Embedded Redis", e);
        }
    }

    @Override
    public void destroy() {
        if (redisServer != null && redisServer.isActive()) {
            try {
                redisServer.stop();
                System.out.println("✓ Embedded Redis stopped");
            } catch (Exception e) {
                System.err.println("⚠ Error stopping Redis: " + e.getMessage());
            }
        }
    }

    // Lettuce용 ConnectionFactory
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", redisPort);
        return factory;
    }

    // RedisTemplate 설정 - RedisUtil에서 사용
    @Bean
    @Primary
    public org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate() {
        org.springframework.data.redis.core.RedisTemplate<String, Object> template = new org.springframework.data.redis.core.RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        return template;
    }

    // Redisson Client 설정 - 분산락 테스트를 위해
    @Bean(destroyMethod = "shutdown")
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:" + redisPort)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        return Redisson.create(config);
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
