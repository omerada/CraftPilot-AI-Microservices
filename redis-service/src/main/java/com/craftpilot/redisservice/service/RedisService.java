package com.craftpilot.redisservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Successfully set key: {} with TTL: {} seconds", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error setting key: {}", key, e);
            throw new RuntimeException("Failed to set value in Redis", e);
        }
    }

    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Retrieved value for key: {}", key);
            return value;
        } catch (Exception e) {
            log.error("Error getting key: {}", key, e);
            throw new RuntimeException("Failed to get value from Redis", e);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting key: {}", key, e);
            throw new RuntimeException("Failed to delete key from Redis", e);
        }
    }

    public boolean acquireLock(String lockKey, long waitTimeMs, long leaseTimeMs) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean acquired = lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("Acquired lock for key: {}", lockKey);
            } else {
                log.debug("Failed to acquire lock for key: {}", lockKey);
            }
            return acquired;
        } catch (Exception e) {
            log.error("Error acquiring lock for key: {}", lockKey, e);
            throw new RuntimeException("Failed to acquire lock", e);
        }
    }

    public void releaseLock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            lock.unlock();
            log.debug("Released lock for key: {}", lockKey);
        } catch (Exception e) {
            log.error("Error releasing lock for key: {}", lockKey, e);
            throw new RuntimeException("Failed to release lock", e);
        }
    }

    public void setWithCache(String mapName, String key, Object value, long ttlSeconds) {
        try {
            RMapCache<String, Object> map = redissonClient.getMapCache(mapName);
            map.put(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Set cached value for map: {}, key: {}, TTL: {} seconds", mapName, key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error setting cached value for map: {}, key: {}", mapName, key, e);
            throw new RuntimeException("Failed to set cached value", e);
        }
    }

    public Object getFromCache(String mapName, String key) {
        try {
            RMapCache<String, Object> map = redissonClient.getMapCache(mapName);
            Object value = map.get(key);
            log.debug("Retrieved cached value for map: {}, key: {}", mapName, key);
            return value;
        } catch (Exception e) {
            log.error("Error getting cached value for map: {}, key: {}", mapName, key, e);
            throw new RuntimeException("Failed to get cached value", e);
        }
    }
} 