package com.craftpilot.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class RemoveBasicAuthHeaderGatewayFilterFactory extends AbstractGatewayFilterFactory<RemoveBasicAuthHeaderGatewayFilterFactory.Config> {

    public RemoveBasicAuthHeaderGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(() -> {
                if (exchange.getResponse().getHeaders().containsKey(HttpHeaders.WWW_AUTHENTICATE)) {
                    String authHeader = exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
                    if (authHeader != null && authHeader.contains("Basic")) {
                        // Basic kısmını kaldır, sadece Bearer kısmını bırak
                        String newAuthHeader = "Bearer realm=\"craftpilot\"";
                        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, newAuthHeader);
                    }
                }
                return exchange.getResponse().setComplete();
            });
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}
