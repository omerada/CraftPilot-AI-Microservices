package com.craftpilot.redisservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import com.craftpilot.redisservice.exception.CacheOperationException;

@Service
@Slf4j
public class RedisCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate, 
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public <T> void set(String key, T value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(key, value, duration);
            meterRegistry.counter("cache.set.success").increment();
            log.debug("Cache set: key={}", key);
        } catch (Exception e) {
            meterRegistry.counter("cache.set.error").increment();
            log.error("Cache set error: key={}, error={}", key, e.getMessage());
            throw new CacheOperationException("Cache set failed", e);
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                meterRegistry.counter("cache.get.miss").increment();
                return Optional.empty();
            }
            meterRegistry.counter("cache.get.hit").increment();
            return Optional.of(objectMapper.convertValue(value, type));
        } catch (Exception e) {
            meterRegistry.counter("cache.get.error").increment();
            log.error("Cache get error: key={}, error={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            meterRegistry.counter("cache.delete.success").increment();
            log.debug("Cache delete: key={}", key);
        } catch (Exception e) {
            meterRegistry.counter("cache.delete.error").increment();
            log.error("Cache delete error: key={}, error={}", key, e.getMessage());
            throw new CacheOperationException("Cache delete failed", e);
        }
    }

    public void deletePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                meterRegistry.counter("cache.delete.pattern.success").increment();
                log.debug("Cache delete pattern: pattern={}, deleted={}", pattern, keys.size());
            }
        } catch (Exception e) {
            meterRegistry.counter("cache.delete.pattern.error").increment();
            log.error("Cache delete pattern error: pattern={}, error={}", pattern, e.getMessage());
            throw new CacheOperationException("Cache delete pattern failed", e);
        }
    }

    public Set<String> getKeys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys : new HashSet<>();
        } catch (Exception e) {
            log.error("Get keys error: pattern={}, error={}", pattern, e.getMessage());
            return new HashSet<>();
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            Properties info = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .info();
            stats.put("info", info);
            stats.put("dbSize", redisTemplate.getConnectionFactory()
                    .getConnection()
                    .dbSize());
            return stats;
        } catch (Exception e) {
            log.error("Get stats error: {}", e.getMessage());
            return stats;
        }
    }
} 