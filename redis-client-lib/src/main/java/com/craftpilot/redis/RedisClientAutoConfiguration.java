package com.craftpilot.redis;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import com.craftpilot.redis.config.RedisClientAutoConfiguration;

/**
 * Redis Client Auto Configuration
 * config paketindeki asıl yapılandırmayı import eder.
 * Bu sınıf geriye dönük uyumluluk için korunmuştur.
 * @deprecated Lütfen com.craftpilot.redis.config.RedisClientAutoConfiguration kullanın
 */
@Deprecated
@Configuration
@Import(RedisClientAutoConfiguration.class)
public class RedisClientAutoConfiguration {
    // Bu sınıf sadece com.craftpilot.redis.config.RedisClientAutoConfiguration'ı import eder
}
