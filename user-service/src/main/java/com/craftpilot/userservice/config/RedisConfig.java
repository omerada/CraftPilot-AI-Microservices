package com.craftpilot.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {

        @Bean
        @Primary
        public ReactiveRedisTemplate<String, Object> userServiceRedisTemplate(
                        @Qualifier("craftPilotReactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory) {

                StringRedisSerializer keySerializer = new StringRedisSerializer();
                Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

                RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder = RedisSerializationContext
                                .newSerializationContext(keySerializer);

                RedisSerializationContext<String, Object> context = builder
                                .value(valueSerializer)
                                .hashKey(keySerializer)
                                .hashValue(valueSerializer)
                                .build();

                return new ReactiveRedisTemplate<>(connectionFactory, context);
        }
}
