package com.craftpilot.apigateway.security;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class RouteMetadataAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        ServerWebExchange exchange = context.getExchange();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        if (route == null) {
            return Mono.just(new AuthorizationDecision(false));
        }

        Map<String, Object> metadata = route.getMetadata();
        boolean secured = metadata.containsKey("secured") ? (boolean) metadata.get("secured") : true;
        List<String> allowedMethods = (List<String>) metadata.getOrDefault("methods", List.of("GET", "POST", "PUT", "DELETE"));
        List<String> requiredRoles = (List<String>) metadata.getOrDefault("roles", List.of("USER"));

        return authentication
            .map(auth -> {
                if (!secured) return true;
                
                // HTTP metod kontrolü
                String requestMethod = exchange.getRequest().getMethod().name();
                if (!allowedMethods.contains(requestMethod)) return false;

                // Rol kontrolü
                boolean hasRequiredRole = auth.getAuthorities().stream()
                    .anyMatch(a -> requiredRoles.contains(a.getAuthority().replace("ROLE_", "")));
                
                return hasRequiredRole;
            })
            .map(AuthorizationDecision::new)
            .defaultIfEmpty(new AuthorizationDecision(false));
    }
}
