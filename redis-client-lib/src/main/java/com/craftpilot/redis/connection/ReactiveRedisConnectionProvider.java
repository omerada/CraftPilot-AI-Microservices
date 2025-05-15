package com.craftpilot.redis.connection;

import com.craftpilot.redis.config.RedisClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

@Slf4j
public class ReactiveRedisConnectionProvider {
    private final ReactiveRedisConnectionFactory connectionFactory;
    private final RedisClientProperties properties;

    public ReactiveRedisConnectionProvider(
            ReactiveRedisConnectionFactory connectionFactory,
            RedisClientProperties properties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
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
     * Executes a Redis operation
     * @param operation Redis operation function to execute
     * @param <T> Return type
     * @return Result of the operation
     */
    public <T> Mono<T> execute(Function<ReactiveRedisConnectionFactory, Mono<T>> operation) {
        return operation.apply(connectionFactory)
                .doOnError(e -> log.error("Redis operation error: {}", e.getMessage()));
    }

    /**
     * Subscribes to Redis channel
     * @param channel Channel name
     * @return Flux of messages
     */
    public Flux<ReactiveSubscription.Message<String, String>> subscribeToChannel(String channel) {
        ByteBuffer channelBuffer = ByteBuffer.wrap(channel.getBytes(StandardCharsets.UTF_8));

        return connectionFactory.getReactiveConnection()
                .pubSubCommands()
                .subscribe(channelBuffer)
                .flatMapMany(subscription -> Flux.<ReactiveSubscription.Message<String, String>>create(sink -> {
                    // This is a placeholder implementation - actual implementation would depend on 
                    // how ReactiveRedisConnectionFactory handles subscriptions
                    log.debug("Subscribed to channel: {}", channel);
                    // Actual subscription handling would happen here
                }));
    }

    /**
     * Subscribes to Redis channel using PubSub
     * @param channel Channel name
     * @return Flux of messages
     */
    public Flux<ReactiveSubscription.Message<String, String>> subscribeToChannelPubSub(String channel) {
        log.info("Subscribing to channel: {}", channel);
        return Flux.<ReactiveSubscription.Message<String, String>>create(sink -> {
            try {
                ByteBuffer channelBuffer = ByteBuffer.wrap(channel.getBytes(StandardCharsets.UTF_8));
                connectionFactory.getReactiveConnection()
                        .pubSubCommands()
                        .subscribe(channelBuffer)
                        .subscribe(
                            subscription -> log.info("Successfully subscribed to channel: {}", channel),
                            error -> {
                                log.error("Error in subscription: {}", error.getMessage(), error);
                                sink.error(error);
                            }
                        );
            } catch (Exception ex) {
                log.error("Error setting up subscription: {}", ex.getMessage(), ex);
                sink.error(ex);
            }
        });
    }
}
