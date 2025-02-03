package com.craftpilot.aiquestionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.craftpilot.aiquestionservice.model.AIModel;
import com.craftpilot.aiquestionservice.model.Question;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${spring.redis.ssl:false}")
    private boolean sslEnabled;

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            configuration.setPassword(redisPassword);
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .useSsl().disablePeerVerification()
            .build();
            
        return new LettuceConnectionFactory(configuration, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, Question> questionRedisTemplate(LettuceConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Question> valueSerializer = new Jackson2JsonRedisSerializer<>(Question.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Question> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Question> context = builder
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, AIModel> aiModelRedisTemplate(LettuceConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<AIModel> valueSerializer = new Jackson2JsonRedisSerializer<>(AIModel.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, AIModel> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, AIModel> context = builder
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
} 