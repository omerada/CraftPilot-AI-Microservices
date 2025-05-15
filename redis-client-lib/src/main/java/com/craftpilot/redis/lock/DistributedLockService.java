package com.craftpilot.redis.lock;

import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface DistributedLockService {

    /**
     * Bir kilit elde etmeye çalışır
     *
     * @param lockKey Kilit anahtarı
     * @param waitTime Bekleme süresi (milisaniye)
     * @param leaseTime Kiralama süresi (milisaniye)
     * @return Kilit elde edilirse true, edilemezse false
     */
    Mono<Boolean> acquireLock(String lockKey, long waitTime, long leaseTime);

    /**
     * Bir kilidi serbest bırakır
     *
     * @param lockKey Kilit anahtarı
     * @return İşlem sonucu
     */
    Mono<Boolean> releaseLock(String lockKey);

    /**
     * Kilit elde ederek bir işlem çalıştırır
     *
     * @param lockKey Kilit anahtarı
     * @param waitTime Bekleme süresi (milisaniye)
     * @param leaseTime Kiralama süresi (milisaniye)
     * @param supplier Çalıştırılacak fonksiyon
     * @param <T> Dönüş tipi
     * @return İşlem sonucu veya kilitlenemezse boş Mono
     */
    <T> Mono<T> executeWithLock(String lockKey, long waitTime, long leaseTime, 
                                Supplier<Mono<T>> supplier);
    
    /**
     * Varsayılan değerlerle kilit elde ederek bir işlem çalıştırır
     *
     * @param lockKey Kilit anahtarı
     * @param supplier Çalıştırılacak fonksiyon
     * @param <T> Dönüş tipi
     * @return İşlem sonucu veya kilitlenemezse boş Mono
     */
    <T> Mono<T> executeWithLock(String lockKey, Supplier<Mono<T>> supplier);
}
