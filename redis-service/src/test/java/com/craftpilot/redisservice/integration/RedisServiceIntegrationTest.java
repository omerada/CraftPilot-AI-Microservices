package com.craftpilot.redisservice.integration;

import com.craftpilot.redisservice.service.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class RedisServiceIntegrationTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @AfterAll
    static void tearDown() {
        if (redis != null && redis.isRunning()) {
            redis.close();
        }
    }

    @Autowired
    private RedisService redisService;

    @Test
    void testSetAndGet() {
        // Given
        String key = "testKey";
        String value = "testValue";
        long ttl = 60;

        // When
        redisService.set(key, value, ttl);
        Object result = redisService.get(key);

        // Then
        assertEquals(value, result);
    }

    @Test
    void testDelete() {
        // Given
        String key = "testKey";
        String value = "testValue";
        long ttl = 60;
        redisService.set(key, value, ttl);

        // When
        redisService.delete(key);
        Object result = redisService.get(key);

        // Then
        assertNull(result);
    }

    @Test
    void testLocking() throws InterruptedException {
        // Given
        String lockKey = "testLock";
        long waitTime = 1000;
        long leaseTime = 5000;

        // When
        boolean acquired = redisService.acquireLock(lockKey, waitTime, leaseTime);

        // Then
        assertTrue(acquired);

        // Release the lock
        redisService.releaseLock(lockKey);
    }

    @Test
    void testCaching() {
        // Given
        String mapName = "testMap";
        String key = "testKey";
        String value = "testValue";
        long ttl = 60;

        // When
        redisService.setWithCache(mapName, key, value, ttl);
        Object result = redisService.getFromCache(mapName, key);

        // Then
        assertEquals(value, result);
    }

    @Test
    void testConcurrentLocking() throws InterruptedException {
        // Given
        String lockKey = "testLock";
        long waitTime = 1000;
        long leaseTime = 5000;

        // When
        boolean firstLock = redisService.acquireLock(lockKey, waitTime, leaseTime);
        boolean secondLock = redisService.acquireLock(lockKey, waitTime, leaseTime);

        // Then
        assertTrue(firstLock);
        assertFalse(secondLock);

        // Release the lock
        redisService.releaseLock(lockKey);
    }

    @Test
    void testCacheExpiration() throws InterruptedException {
        // Given
        String mapName = "testMap";
        String key = "testKey";
        String value = "testValue";
        long ttl = 1; // 1 second TTL

        // When
        redisService.setWithCache(mapName, key, value, ttl);
        Object resultBeforeExpiration = redisService.getFromCache(mapName, key);
        
        // Wait for expiration
        Thread.sleep(2000);
        Object resultAfterExpiration = redisService.getFromCache(mapName, key);

        // Then
        assertEquals(value, resultBeforeExpiration);
        assertNull(resultAfterExpiration);
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 100;
        String key = "concurrentKey";
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCounter = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (redisService.acquireLock(key, 100, 1000)) {
                            try {
                                successCounter.incrementAndGet();
                            } finally {
                                redisService.releaseLock(key);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        completionLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(successCounter.get() > 0);
        assertTrue(successCounter.get() <= threadCount * operationsPerThread);
    }

    @Test
    void testBulkOperations() {
        // Given
        int itemCount = 1000;
        String mapName = "bulkMap";

        // When - Bulk Set
        for (int i = 0; i < itemCount; i++) {
            redisService.setWithCache(mapName, "key" + i, "value" + i, 60);
        }

        // Then - Verify all items
        for (int i = 0; i < itemCount; i++) {
            Object result = redisService.getFromCache(mapName, "key" + i);
            assertEquals("value" + i, result);
        }
    }
} 