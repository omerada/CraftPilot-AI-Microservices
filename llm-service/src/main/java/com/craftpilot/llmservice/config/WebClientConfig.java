package com.craftpilot.llmservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${openrouter.api.url}")
    private String openRouterApiUrl;

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    @Bean
    public WebClient openRouterWebClient() {
        // ConnectionProvider'ı optimize edelim
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();

        // HttpClient'ı optimize edelim
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(60))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)))
                .wiretap(true);  // Debug için network operasyonlarını logla

        // Buffer boyutlarını arttıralım
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        // WebClient yap
        return WebClient.builder()
                .baseUrl(openRouterApiUrl)
                .defaultHeader("Authorization", "Bearer " + openRouterApiKey)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .filter(logRequest())
                .build();
    }

    // İstek logları için yardımcı metot
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (clientRequest.url().toString().contains("stream")) {
                System.out.println("Stream Request URL: " + clientRequest.url());
                System.out.println("Stream Request Method: " + clientRequest.method());
                System.out.println("Stream Request Headers: " + clientRequest.headers());
            }
            return Mono.just(clientRequest);
        });
    }
}