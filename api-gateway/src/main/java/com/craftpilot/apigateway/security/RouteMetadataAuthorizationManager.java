@Slf4j
@Component
public class RouteMetadataAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {
    
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        ServerWebExchange exchange = context.getExchange();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        if (route == null) {
            log.debug("No route found for path: {}", exchange.getRequest().getPath());
            return Mono.just(new AuthorizationDecision(false));
        }

        Map<String, Object> metadata = route.getMetadata();
        boolean secured = metadata.containsKey("secured") ? (boolean) metadata.get("secured") : true;
        
        if (!secured || SecurityConstants.isPublicPath(exchange.getRequest().getPath().value())) {
            return Mono.just(new AuthorizationDecision(true));
        }

        return authentication
            .filter(Authentication::isAuthenticated)
            .map(auth -> {
                // Method kontrolü
                String requestMethod = exchange.getRequest().getMethod().name();
                List<String> allowedMethods = (List<String>) metadata.getOrDefault("methods", 
                    Arrays.asList("GET", "POST", "PUT", "DELETE"));
                
                if (!allowedMethods.contains(requestMethod)) {
                    log.debug("Method {} not allowed for path {}", requestMethod, exchange.getRequest().getPath());
                    return false;
                }

                // Rol kontrolü
                List<String> requiredRoles = (List<String>) metadata.getOrDefault("roles", List.of("USER"));
                boolean hasRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(role -> role.replace("ROLE_", ""))
                    .anyMatch(requiredRoles::contains);

                if (!hasRole) {
                    log.debug("User {} does not have required roles {} for path {}", 
                        auth.getName(), requiredRoles, exchange.getRequest().getPath());
                }

                return hasRole;
            })
            .defaultIfEmpty(false)
            .map(AuthorizationDecision::new);
    }
}
