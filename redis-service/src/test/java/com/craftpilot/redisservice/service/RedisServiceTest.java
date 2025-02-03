package com.craftpilot.redisservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RLock rLock;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisService = new RedisService(redisTemplate, redissonClient);
    }

    @Test
    void testSet() {
        // Given
        String key = "testKey";
        String value = "testValue";
        long ttl = 60;

        // When
        redisService.set(key, value, ttl);

        // Then
        verify(valueOperations).set(key, value, ttl, TimeUnit.SECONDS);
    }

    @Test
    void testGet() {
        // Given
        String key = "testKey";
        String expectedValue = "testValue";
        when(valueOperations.get(key)).thenReturn(expectedValue);

        // When
        Object result = redisService.get(key);

        // Then
        assertEquals(expectedValue, result);
        verify(valueOperations).get(key);
    }

    @Test
    void testDelete() {
        // Given
        String key = "testKey";

        // When
        redisService.delete(key);

        // Then
        verify(redisTemplate).delete(key);
    }

    @Test
    void testAcquireLock() throws InterruptedException {
        // Given
        String lockKey = "testLock";
        long waitTime = 1000;
        long leaseTime = 5000;
        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS)).thenReturn(true);

        // When
        boolean result = redisService.acquireLock(lockKey, waitTime, leaseTime);

        // Then
        assertTrue(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
    }

    @Test
    void testAcquireLockWhenLocked() throws InterruptedException {
        // Given
        String lockKey = "testLock";
        long waitTime = 1000;
        long leaseTime = 5000;
        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS)).thenReturn(false);

        // When
        boolean result = redisService.acquireLock(lockKey, waitTime, leaseTime);

        // Then
        assertFalse(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
    }

    @Test
    void testReleaseLock() {
        // Given
        String lockKey = "testLock";
        when(redissonClient.getLock(lockKey)).thenReturn(rLock);

        // When
        redisService.releaseLock(lockKey);

        // Then
        verify(redissonClient).getLock(lockKey);
        verify(rLock).unlock();
    }

    @Test
    void testSetWithCache() {
        // Given
        String mapName = "testMap";
        String key = "testKey";
        String value = "testValue";
        long ttl = 60;
        @SuppressWarnings("unchecked")
        RMapCache<String, Object> mapCache = mock(RMapCache.class);
        when(redissonClient.<String, Object>getMapCache(mapName)).thenReturn(mapCache);

        // When
        redisService.setWithCache(mapName, key, value, ttl);

        // Then
        verify(redissonClient).getMapCache(mapName);
        verify(mapCache).put(key, value, ttl, TimeUnit.SECONDS);
    }

    @Test
    void testGetFromCache() {
        // Given
        String mapName = "testMap";
        String key = "testKey";
        String expectedValue = "testValue";
        @SuppressWarnings("unchecked")
        RMapCache<String, Object> mapCache = mock(RMapCache.class);
        when(redissonClient.<String, Object>getMapCache(mapName)).thenReturn(mapCache);
        when(mapCache.get(key)).thenReturn(expectedValue);

        // When
        Object result = redisService.getFromCache(mapName, key);

        // Then
        assertEquals(expectedValue, result);
        verify(redissonClient).getMapCache(mapName);
        verify(mapCache).get(key);
    }

    @Test
    void testGetFromCacheWhenNotExists() {
        // Given
        String mapName = "testMap";
        String key = "testKey";
        RMapCache<String, Object> mapCache = mock(RMapCache.class);
        when(redissonClient.<String, Object>getMapCache(anyString())).thenReturn(mapCache);
        when(mapCache.get(anyString())).thenReturn(null);

        // When
        Object result = redisService.getFromCache(mapName, key);

        // Then
        assertNull(result);
        verify(redissonClient).getMapCache(mapName);
        verify(mapCache).get(key);
    }

    @Test
    void testSetWithException() {
        // Given
        String key = "testKey";
        String value = "testValue";
        long ttl = 60;
        doThrow(new RuntimeException("Redis error")).when(valueOperations).set(anyString(), any(), anyLong(), any());

        // When & Then
        assertThrows(RuntimeException.class, () -> redisService.set(key, value, ttl));
        verify(valueOperations).set(key, value, ttl, TimeUnit.SECONDS);
    }

    @Test
    void testGetWithException() {
        // Given
        String key = "testKey";
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> redisService.get(key));
        verify(valueOperations).get(key);
    }
} 