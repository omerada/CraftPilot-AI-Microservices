package com.craftpilot.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("CraftPilot API Gateway")
                        .description("API Gateway for CraftPilot AI Platform")
                        .version("1.0.0"));
    }

    @Bean
    public List<GroupedOpenApi> apis(RouteDefinitionLocator locator) {
        List<GroupedOpenApi> groups = new ArrayList<>();
        
        locator.getRouteDefinitions().subscribe(routeDefinition -> {
            String name = routeDefinition.getId().replaceAll("-service", "");
            groups.add(GroupedOpenApi.builder()
                    .pathsToMatch("/" + name + "/**")
                    .group(name)
                    .build());
        });
        
        return groups;
    }
}
