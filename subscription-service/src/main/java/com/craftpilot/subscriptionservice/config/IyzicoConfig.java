package com.craftpilot.subscriptionservice.config;

import com.iyzipay.Options;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter; 
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "iyzico")
public class IyzicoConfig {

    private String apiKey;
    private String secretKey;
    private String baseUrl;
    private String callbackUrl;

    private final MeterRegistry meterRegistry;

    @Bean
    public Options options() {
        Options options = new Options();
        options.setApiKey(apiKey);
        options.setSecretKey(secretKey);
        options.setBaseUrl(baseUrl);
        return options;
    }

    @Bean
    public Counter iyzicoSuccessCounter() {
        return Counter.builder("iyzico.payment.success")
                .description("Number of successful Iyzico payments")
                .register(meterRegistry);
    }

    @Bean
    public Counter iyzicoFailureCounter() {
        return Counter.builder("iyzico.payment.failure")
                .description("Number of failed Iyzico payments")
                .register(meterRegistry);
    }
} 