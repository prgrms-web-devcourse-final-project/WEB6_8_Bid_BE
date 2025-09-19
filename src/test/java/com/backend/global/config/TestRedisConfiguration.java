package com.backend.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

@TestConfiguration
@ActiveProfiles("test")
public class TestRedisConfiguration {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        int redisPort = findAvailablePort();
        redisServer = new RedisServer(redisPort);
        redisServer.start();

        System.setProperty("spring.data.redis.port", String.valueOf(redisPort));
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (IOException e) {
                // Log the exception or rethrow as RuntimeException if necessary
                System.err.println("Error stopping Redis server: " + e.getMessage());
            }
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
