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
import reactor.netty.tcp.TcpClient;

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
        // HttpClient yapılandırması
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .responseTimeout(Duration.ofSeconds(120))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS));
                });

        // Exchange strategies yapılandırması
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(8 * 1024 * 1024))
                .build();

        // WebClient yapılandırması ve dönüşü
        return WebClient.builder()
                .baseUrl(openRouterApiUrl)
                .defaultHeader("Authorization", "Bearer " + openRouterApiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Cache-Control", "no-cache")
                .defaultHeader("Connection", "keep-alive")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .filter(logRequest())
                .build();
    }

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