package com.craftpilot.llmservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1}")
    private String openRouterApiUrl;

    @Value("${openrouter.requestTimeoutSeconds:60}")
    private int requestTimeoutSeconds;

    @Value("${webclient.max-in-memory-size:10485760}")
    private int maxInMemorySize;

    @Value("${webclient.connection.max-connections:50}")
    private int maxConnections;

    @Value("${webclient.connection.acquire-timeout:15}")
    private int connectionAcquireTimeoutSeconds;

    @Value("${webclient.connection.max-idle-time:30}")
    private int connectionMaxIdleTimeSeconds;

    @Value("${webclient.connection.max-life-time:300}")
    private int connectionMaxLifeTimeSeconds;

    @Value("${webclient.connection.eviction-interval:120}")
    private int connectionEvictionIntervalSeconds;

    @Bean
    public WebClient openRouterWebClient() {
        // Bellek yönetimi optimization - makul bir boyut
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .build();

        // Optimize edilmiş connection provider
        ConnectionProvider provider = ConnectionProvider.builder("openrouter-connection-pool")
                .maxConnections(maxConnections)
                .maxIdleTime(Duration.ofSeconds(connectionMaxIdleTimeSeconds))
                .maxLifeTime(Duration.ofMinutes(connectionMaxLifeTimeSeconds / 60))
                .pendingAcquireTimeout(Duration.ofSeconds(connectionAcquireTimeoutSeconds))
                .evictInBackground(Duration.ofSeconds(connectionEvictionIntervalSeconds))
                .build();

        // HTTP client optimization
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, requestTimeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(requestTimeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(requestTimeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(requestTimeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(openRouterApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("HTTP-Referer", "https://craftpilot.io")
                .defaultHeader("X-Title", "Craft Pilot AI")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openRouterApiKey)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(15))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        // Memory limit'ini arttıralım (default: 256KB)
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies);
    }
}