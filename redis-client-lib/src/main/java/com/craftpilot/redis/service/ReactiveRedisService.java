package com.craftpilot.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ReactiveRedisService {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * Get a value from Redis
     * @param key Redis key
     * @return Value as Mono
     */
    public Mono<Object> get(String key) {
        return redisTemplate.opsForValue().get(key)
                .doOnSubscribe(s -> log.debug("Getting value for key: {}", key))
                .doOnNext(value -> log.debug("Got value for key {}: {}", key, value));
    }

    /**
     * Get a value from Redis with type
     * @param key Redis key
     * @param clazz Type class
     * @return Value as Mono
     */
    public <T> Mono<T> get(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key)
                .cast(clazz)
                .doOnSubscribe(s -> log.debug("Getting value for key: {} as {}", key, clazz.getSimpleName()))
                .doOnNext(value -> log.debug("Got value for key {}: {}", key, value));
    }

    /**
     * Set a value in Redis
     * @param key Redis key
     * @param value Value to set
     * @return Result of operation
     */
    public Mono<Boolean> set(String key, Object value) {
        return redisTemplate.opsForValue().set(key, value)
                .doOnSubscribe(s -> log.debug("Setting value for key: {}", key))
                .doOnNext(result -> log.debug("Set value for key {}: {}", key, result));
    }

    /**
     * Set a value in Redis with expiration
     * @param key Redis key
     * @param value Value to set
     * @param timeout Expiration time
     * @return Result of operation
     */
    public Mono<Boolean> set(String key, Object value, Duration timeout) {
        // Timeout süresi için geçerlilik kontrolü
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            return Mono.error(new IllegalArgumentException("Timeout must be a positive duration"));
        }
        
        return redisTemplate.opsForValue()
                .set(key, value, timeout)
                .doOnSubscribe(s -> log.debug("Setting value for key: {} with timeout: {}", key, timeout))
                .doOnNext(result -> log.debug("Set value for key {} with timeout {}: {}", key, timeout, result));
    }

    /**
     * Conditionally set a key with timeout
     */
    public <T> Mono<Boolean> setIfNotExists(String key, T value, Duration timeout) {
        // Convert Duration to seconds for comparison
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            return redisTemplate.opsForValue().setIfAbsent(key, value);
        }
        
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * Delete a key from Redis
     * @param key Redis key
     * @return Result of operation
     */
    public Mono<Boolean> delete(String key) {
        return redisTemplate.opsForValue().delete(key)
                .doOnSubscribe(s -> log.debug("Deleting key: {}", key))
                .doOnNext(result -> log.debug("Deleted key {}: {}", key, result));
    }

    /**
     * Check if a key exists in Redis
     * @param key Redis key
     * @return true if key exists, false otherwise
     */
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key)
                .doOnSubscribe(s -> log.debug("Checking if key exists: {}", key))
                .doOnNext(result -> log.debug("Key {} exists: {}", key, result));
    }

    /**
     * Set key expiration
     * @param key Redis key
     * @param timeout Expiration time
     * @return Result of operation
     */
    public Mono<Boolean> expire(String key, Duration timeout) {
        // Timeout süresi için geçerlilik kontrolü
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            return Mono.error(new IllegalArgumentException("Timeout must be a positive duration"));
        }
        
        return redisTemplate.expire(key, timeout)
                .doOnSubscribe(s -> log.debug("Setting expiration for key: {}, timeout: {}", key, timeout))
                .doOnNext(result -> log.debug("Set expiration for key {}: {}", key, result));
    }

    /**
     * Get key expiration
     * @param key Redis key
     * @return Expiration time in seconds
     */
    public Mono<Duration> getExpire(String key) {
        return redisTemplate.getExpire(key)
            .map(seconds -> {
                if (seconds == null || seconds <= 0L) return Duration.ZERO;
                return Duration.ofSeconds(seconds);
            })
            .doOnSubscribe(s -> log.debug("Getting expiration for key: {}", key))
            .doOnNext(expire -> log.debug("Got expiration for key {}: {}", key, expire));
    }

    /**
     * Increment a key
     * @param key Redis key
     * @return New value
     */
    public Mono<Long> increment(String key) {
        return redisTemplate.opsForValue().increment(key)
                .doOnSubscribe(s -> log.debug("Incrementing key: {}", key))
                .doOnNext(value -> log.debug("Incremented key {}: {}", key, value));
    }

    /**
     * Publish a message to a channel
     * @param channel Channel name
     * @param message Message to publish
     * @return Number of subscribers that received the message
     */
    public Mono<Long> publish(String channel, Object message) {
        return redisTemplate.convertAndSend(channel, message)
                .doOnSubscribe(s -> log.debug("Publishing to channel: {}", channel))
                .doOnNext(subscribers -> log.debug("Published to channel {}: {} subscribers", channel, subscribers));
    }

    /**
     * Hash operations - Get all entries
     * @param key Redis key
     * @return Map of hash entries
     */
    public Mono<Map<Object, Object>> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key)
                .collectMap(entry -> entry.getKey(), entry -> entry.getValue())
                .doOnSubscribe(s -> log.debug("Getting all hash entries for key: {}", key))
                .doOnNext(entries -> log.debug("Got {} hash entries for key {}", entries.size(), key));
    }

    /**
     * Hash operations - Set a hash entry
     * @param key Redis key
     * @param hashKey Hash key
     * @param value Value to set
     * @return Result of operation
     */
    public Mono<Boolean> hSet(String key, Object hashKey, Object value) {
        return redisTemplate.opsForHash().put(key, hashKey, value)
                .doOnSubscribe(s -> log.debug("Setting hash entry for key: {}, hashKey: {}", key, hashKey))
                .doOnNext(result -> log.debug("Set hash entry for key {}, hashKey {}: {}", key, hashKey, result));
    }

    /**
     * Hash operations - Get a hash entry
     * @param key Redis key
     * @param hashKey Hash key
     * @return Hash entry value
     */
    public Mono<Object> hGet(String key, Object hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey)
                .doOnSubscribe(s -> log.debug("Getting hash entry for key: {}, hashKey: {}", key, hashKey))
                .doOnNext(value -> log.debug("Got hash entry for key {}, hashKey {}: {}", key, hashKey, value));
    }

    /**
     * List operations - Push to list
     * @param key Redis key
     * @param value Value to push
     * @return New list size
     */
    public Mono<Long> lPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value)
                .doOnSubscribe(s -> log.debug("Pushing to list for key: {}", key))
                .doOnNext(size -> log.debug("Pushed to list for key {}, new size: {}", key, size));
    }

    /**
     * List operations - Get list range
     * @param key Redis key
     * @param start Start index
     * @param end End index
     * @return List elements
     */
    public Flux<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end)
                .doOnSubscribe(s -> log.debug("Getting list range for key: {}, start: {}, end: {}", key, start, end))
                .doOnComplete(() -> log.debug("Got list range for key {}, start: {}, end: {}", key, start, end));
    }

    /**
     * Redis sunucusuna ping gönderir
     * @return Ping başarılı ise true, değilse false
     */
    public Mono<Boolean> ping() {
        return redisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .ping()
                .map(pong -> "PONG".equalsIgnoreCase(pong))
                .doOnSubscribe(s -> log.debug("Redis'e ping gönderiliyor"))
                .doOnNext(result -> log.debug("Redis ping sonucu: {}", result))
                .doOnError(e -> log.error("Redis ping hatası: {}", e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Redis server sağlık durumunu kontrol eder
     * @return Redis server sağlıklı ise true, değilse false
     */
    public boolean isRedisHealthy() {
        try {
            return redisTemplate.getConnectionFactory() != null &&
                   redisTemplate.getConnectionFactory().getReactiveConnection() != null;
        } catch (Exception e) {
            log.error("Redis sağlık kontrolü başarısız: {}", e.getMessage());
            return false;
        }
    }
}
