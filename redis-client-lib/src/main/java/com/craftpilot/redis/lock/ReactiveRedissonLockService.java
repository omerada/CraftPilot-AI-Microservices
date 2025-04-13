package com.craftpilot.redis.lock;

import com.craftpilot.redis.exception.RedisOperationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class ReactiveRedissonLockService implements DistributedLockService {

    private final RedissonClient redissonClient;
    private final CircuitBreaker circuitBreaker;
    
    // Varsayılan değerler
    private static final long DEFAULT_WAIT_TIME_MS = 2000;
    private static final long DEFAULT_LEASE_TIME_MS = 5000;

    public ReactiveRedissonLockService(
            RedissonClient redissonClient,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.redissonClient = redissonClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("redisLock");
    }

    @Override
    public Mono<Boolean> acquireLock(String lockKey, long waitTime, long leaseTime) {
        log.debug("Dağıtık kilit elde edilmeye çalışılıyor: key={}, waitTime={}, leaseTime={}", 
                lockKey, waitTime, leaseTime);
                
        return Mono.fromCallable(() -> {
            try {
                RLock lock = redissonClient.getLock(lockKey);
                boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
                
                if (acquired) {
                    log.debug("Dağıtık kilit başarıyla elde edildi: key={}", lockKey);
                } else {
                    log.debug("Dağıtık kilit elde edilemedi: key={}", lockKey);
                }
                
                return acquired;
            } catch (InterruptedException e) {
                log.warn("Dağıtık kilit elde etme işlemi kesintiye uğradı: key={}", lockKey);
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                log.error("Dağıtık kilit elde etme hatası: key={}, error={}", lockKey, e.getMessage());
                throw new RedisOperationException("Dağıtık kilit elde etme hatası", e);
            }
        })
        .transform(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> releaseLock(String lockKey) {
        log.debug("Dağıtık kilit serbest bırakılıyor: key={}", lockKey);
        
        return Mono.fromCallable(() -> {
            try {
                RLock lock = redissonClient.getLock(lockKey);
                
                if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("Dağıtık kilit başarıyla serbest bırakıldı: key={}", lockKey);
                    return true;
                } else {
                    log.warn("Dağıtık kilit bu thread tarafından tutulmuyor: key={}", lockKey);
                    return false;
                }
            } catch (Exception e) {
                log.error("Dağıtık kilit serbest bırakma hatası: key={}, error={}", lockKey, e.getMessage());
                throw new RedisOperationException("Dağıtık kilit serbest bırakma hatası", e);
            }
        })
        .transform(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorReturn(false);
    }

    @Override
    public <T> Mono<T> executeWithLock(String lockKey, long waitTime, long leaseTime, 
                                       Supplier<Mono<T>> supplier) {
        return acquireLock(lockKey, waitTime, leaseTime)
                .flatMap(acquired -> {
                    if (acquired) {
                        return supplier.get()
                                .doFinally(signalType -> 
                                    releaseLock(lockKey).subscribe(
                                        released -> {
                                            if (!released) {
                                                log.warn("Kilit serbest bırakılamadı: key={}", lockKey);
                                            }
                                        }
                                    )
                                );
                    } else {
                        return Mono.empty();
                    }
                });
    }

    @Override
    public <T> Mono<T> executeWithLock(String lockKey, Supplier<Mono<T>> supplier) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME_MS, DEFAULT_LEASE_TIME_MS, supplier);
    }
}
