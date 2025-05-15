package com.craftpilot.redis.connection;

import com.craftpilot.redis.config.RedisClientProperties;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

@Slf4j
public class ReactiveRedisConnectionProvider {
    private final ReactiveRedisConnectionFactory connectionFactory;
    private final RedisClientProperties properties;
    private final Retry connectionRetry;

    public ReactiveRedisConnectionProvider(
            ReactiveRedisConnectionFactory connectionFactory,
            RedisClientProperties properties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
        
        // Create retry configuration if enabled
        if (properties.getRetry().isEnabled()) {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(properties.getRetry().getMaxAttempts())
                    .waitDuration(properties.getRetry().getBackoff())
                    .build();
            this.connectionRetry = Retry.of("redis-connection", retryConfig);
        } else {
            this.connectionRetry = null;
        }
    }

    /**
     * Checks if Redis is available
     * @return true if Redis is available, false otherwise
     */
    public Mono<Boolean> isAvailable() {
        return connectionFactory.getReactiveConnection()
                .ping()
                .map(pong -> true)
                .onErrorReturn(false)
                .timeout(properties.getConnectTimeout(), Mono.just(false));
    }

    /**
     * Executes a Redis operation with retry logic if enabled
     * @param operation Redis operation function to execute
     * @param <T> Return type
     * @return Result of the operation
     */
    public <T> Mono<T> executeWithRetry(Function<ReactiveRedisConnectionFactory, Mono<T>> operation) {
        Mono<T> operationMono = operation.apply(connectionFactory);
        
        if (connectionRetry != null) {
            return Retry.decoratePublisher(operationMono, connectionRetry);
        }
        
        return operationMono;
    }

    /**
     * Subscribes to Redis channel
     * @param channel Channel name
     * @return Flux of messages
     */
    public Flux<ReactiveSubscription.Message<String, String>> subscribeToChannel(String channel) {
        return connectionFactory.getReactiveConnection()
                .pubSubCommands()
                .subscribe(channel);
    }
}
