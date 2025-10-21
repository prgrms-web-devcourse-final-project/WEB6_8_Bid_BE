package com.backend.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setData(String key, String value, Long expiredTime) {
        redisTemplate.opsForValue().set(key, value, expiredTime, TimeUnit.MILLISECONDS);
    }

    public Object getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    public void setDataExpire(String key, Object value, Long expiredTimeSeconds) {
        redisTemplate.opsForValue().set(key, value, expiredTimeSeconds, TimeUnit.SECONDS);
    }

    public Boolean existData(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long setSAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    public Long setSRem(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    public Set<Object> setSMembers(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        return members != null ? members : Set.of();
    }
}
