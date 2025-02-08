package com.craftpilot.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI craftPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CraftPilot API Gateway")
                        .description("CraftPilot Microservices API Documentation")
                        .version("1.0"));
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", matchIfMissing = true)
    public SwaggerUiConfigParameters swaggerUiConfigParameters() {
        return new SwaggerUiConfigParameters();
    }

    @Bean
    public List<GroupedOpenApi> apis(SwaggerUiConfigParameters swaggerUiConfigParameters,
                                    RouteDefinitionLocator locator) {
        List<GroupedOpenApi> groups = new ArrayList<>();
        
        locator.getRouteDefinitions().collectList().block()
                .stream()
                .filter(routeDefinition -> routeDefinition.getId().matches(".*-service"))
                .forEach(routeDefinition -> {
                    String name = routeDefinition.getId().replaceAll("-service", "");
                    swaggerUiConfigParameters.addGroup(name);
                    GroupedOpenApi.builder()
                            .pathsToMatch("/" + name + "/**")
                            .group(name)
                            .build();
                });
                
        return groups;
    }
}
