package com.craftpilot.aiquestionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;
import org.slf4j.MDC;

import java.util.UUID;

@Configuration
public class LoggingConfig {

    @Bean
    public WebFilter traceIdFilter() {
        return (exchange, chain) -> {
            String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-ID");
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
            }
            
            MDC.put("traceId", traceId);
            MDC.put("userId", exchange.getRequest().getHeaders().getFirst("X-User-ID"));
            
            return chain.filter(exchange)
                    .doFinally(signalType -> MDC.clear());
        };
    }
} 