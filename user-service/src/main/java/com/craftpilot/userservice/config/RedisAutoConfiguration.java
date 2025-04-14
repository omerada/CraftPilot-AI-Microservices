package com.craftpilot.userservice.config;

import com.craftpilot.redis.RedisClientAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Redis Client Lib için otomatik yapılandırma sınıfı.
 * Bu sınıf, redis-client-lib kütüphanesinin otomatik yapılandırmasını aktifleştirir.
 */
@Configuration
@ConditionalOnClass(RedisClientAutoConfiguration.class)
@Import(RedisClientAutoConfiguration.class)
public class RedisAutoConfiguration {
    // Redis client lib'in otomatik konfigürasyonunu import ediyor
}
