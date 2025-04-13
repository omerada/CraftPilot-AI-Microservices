package com.craftpilot.redis.connection;

import com.craftpilot.redis.config.RedisClientProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

@Slf4j
public class ReactiveRedisConnectionProvider {

    private final RedisClientProperties properties;

    public ReactiveRedisConnectionProvider(RedisClientProperties properties) {
        this.properties = properties;
    }

    public ReactiveRedisConnectionFactory createConnectionFactory() {
        log.info("Redis bağlantısı oluşturuluyor: {}:{}", properties.getHost(), properties.getPort());
        
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(properties.getHost());
        redisConfig.setPort(properties.getPort());
        redisConfig.setDatabase(properties.getDatabase());
        
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            redisConfig.setPassword(properties.getPassword());
        }
        
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(properties.getPoolMaxActive());
        poolConfig.setMaxIdle(properties.getPoolMaxIdle());
        poolConfig.setMinIdle(properties.getPoolMinIdle());
        
        Duration maxWait = properties.getPoolMaxWait();
        if (maxWait.isNegative()) {
            poolConfig.setMaxWait(-1);
        } else {
            poolConfig.setMaxWait(maxWait);
        }
        
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();
        
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(properties.getTimeout())
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .build();
        
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
}
