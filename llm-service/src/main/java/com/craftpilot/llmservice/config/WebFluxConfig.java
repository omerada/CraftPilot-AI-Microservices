@Configuration
public class WebFluxConfig {
    
    @Bean
    public WebFilter responseHeaderFilter() {
        return (exchange, chain) -> {
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return chain.filter(exchange);
        };
    }
}
