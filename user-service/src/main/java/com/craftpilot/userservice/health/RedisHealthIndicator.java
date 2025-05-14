package com.craftpilot.userservice.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @deprecated Bu sınıf artık kullanılmamaktadır. Redis Client Library kendi health indicator'ını sağlamaktadır.
 */
@Deprecated
@Slf4j
@Component("legacyRedisHealthIndicator") // Bean adını değiştirdim, çakışmayı önlemek için
@ConditionalOnProperty(name = "craftpilot.redis.legacy-health-indicator-enabled", havingValue = "true", matchIfMissing = false)
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    @Override
    public Mono<Health> health() {
        // Redis Client Library kendi health indicator'ını sağladığı için
        // bu sınıf artık kullanılmamaktadır
        log.info("Using Redis Client Library health indicator instead");
        return Mono.just(Health.up().build());
    }
}
