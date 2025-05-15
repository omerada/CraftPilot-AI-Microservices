package com.craftpilot.redis.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ReactiveRedisRepository {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    // Default TTL for entities
    private static final Duration DEFAULT_TTL = Duration.ofDays(1);

    /**
     * Saves an entity with the given ID
     * @param id Entity ID
     * @param entity Entity to save
     * @return Result of operation
     */
    public <T> Mono<Boolean> save(String id, T entity) {
        return save(id, entity, DEFAULT_TTL);
    }

    /**
     * Saves an entity with the given ID and custom TTL
     * @param id Entity ID
     * @param entity Entity to save
     * @param ttl Time to live
     * @return Result of operation
     */
    public <T> Mono<Boolean> save(String id, T entity, Duration ttl) {
        return redisTemplate.opsForValue().set(id, entity, ttl)
                .doOnSubscribe(s -> log.debug("Saving entity with ID: {} and TTL: {}", id, ttl))
                .doOnNext(result -> log.debug("Saved entity with ID {}: {}", id, result));
    }

    /**
     * Finds an entity by ID
     * @param id Entity ID
     * @return Entity as Mono
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> findById(String id, Class<T> entityClass) {
        return redisTemplate.opsForValue().get(id)
                .cast(entityClass)
                .doOnSubscribe(s -> log.debug("Finding entity with ID: {}", id))
                .doOnNext(entity -> log.debug("Found entity with ID {}: {}", id, entity != null));
    }

    /**
     * Deletes an entity by ID
     * @param id Entity ID
     * @return Result of operation
     */
    public Mono<Boolean> deleteById(String id) {
        return redisTemplate.delete(id)
                .map(count -> count > 0)
                .doOnSubscribe(s -> log.debug("Deleting entity with ID: {}", id))
                .doOnNext(result -> log.debug("Deleted entity with ID {}: {}", id, result));
    }

    /**
     * Finds entities by multiple IDs
     * @param ids Entity IDs
     * @return Flux of entities
     */
    public <T> Flux<T> findAllById(List<String> ids, Class<T> entityClass) {
        return Flux.fromIterable(ids)
                .flatMap(id -> findById(id, entityClass))
                .doOnSubscribe(s -> log.debug("Finding entities with IDs: {}", ids));
    }

    /**
     * Checks if an entity exists
     * @param id Entity ID
     * @return true if entity exists, false otherwise
     */
    public Mono<Boolean> existsById(String id) {
        return redisTemplate.hasKey(id)
                .doOnSubscribe(s -> log.debug("Checking if entity exists with ID: {}", id))
                .doOnNext(result -> log.debug("Entity with ID {} exists: {}", id, result));
    }

    /**
     * Updates TTL for an entity
     * @param id Entity ID
     * @param ttl New TTL
     * @return Result of operation
     */
    public Mono<Boolean> updateTtl(String id, Duration ttl) {
        return redisTemplate.expire(id, ttl)
                .doOnSubscribe(s -> log.debug("Updating TTL for entity with ID: {} to {}", id, ttl))
                .doOnNext(result -> log.debug("Updated TTL for entity with ID {}: {}", id, result));
    }

    /**
     * Prefix ve identifier ile cache key oluşturur
     * @param prefix Key prefix
     * @param identifier Benzersiz tanımlayıcı
     * @return Oluşturulan cache key
     */
    public String generateKey(String prefix, String identifier) {
        return prefix + ":" + identifier;
    }
    
    /**
     * Entity'yi cache'den alır
     * @param key Cache key
     * @param entityClass Entity sınıfı
     * @return Entity veya boş Mono
     */
    public <T> Mono<T> get(String key, Class<T> entityClass) {
        return redisTemplate.opsForValue().get(key)
                .cast(entityClass)
                .doOnSubscribe(s -> log.debug("Getting entity from cache: key={}", key))
                .doOnNext(entity -> log.debug("Found entity in cache: key={}", key))
                .doOnError(e -> log.error("Error getting entity from cache: key={}, error={}", key, e.getMessage()));
    }
    
    /**
     * Entity'yi cache'e kaydeder
     * @param key Cache key
     * @param entity Entity
     * @param ttl Cache timeout
     * @return İşlem sonucu
     */
    public <T> Mono<Boolean> set(String key, T entity, Duration ttl) {
        return redisTemplate.opsForValue().set(key, entity, ttl)
                .doOnSubscribe(s -> log.debug("Setting entity in cache: key={}, ttl={}", key, ttl))
                .doOnNext(result -> log.debug("Set entity in cache: key={}, success={}", key, result))
                .doOnError(e -> log.error("Error setting entity in cache: key={}, error={}", key, e.getMessage()));
    }
    
    /**
     * Entity'yi cache'den siler
     * @param key Cache key
     * @return İşlem sonucu
     */
    public Mono<Boolean> delete(String key) {
        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSubscribe(s -> log.debug("Deleting entity from cache: key={}", key))
                .doOnNext(result -> log.debug("Deleted entity from cache: key={}, success={}", key, result))
                .doOnError(e -> log.error("Error deleting entity from cache: key={}, error={}", key, e.getMessage()));
    }
}
