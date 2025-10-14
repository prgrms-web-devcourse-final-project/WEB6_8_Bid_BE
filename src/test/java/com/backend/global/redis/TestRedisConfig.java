package com.backend.global.redis;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class TestRedisConfig {

    private final RedisServer redisServer;

    public TestRedisConfig(@Value("${spring.data.redis.port}") int redisPort) {
        try {
            this.redisServer = new RedisServer(redisPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void startRedis() {
        try {
            this.redisServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void stopRedis() {
        try {
            this.redisServer.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(@Value("${spring.data.redis.host}") String host, @Value("${spring.data.redis.port}") int port) {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
