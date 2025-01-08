package com.craftpilot.authservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * The entry point for the API Gateway Spring Boot application.
 * This application is a Eureka client that registers itself with a Eureka server.
 * The application is configured with the {@link SpringBootApplication} annotation.
 */
@SpringBootApplication
@EnableFeignClients
@OpenAPIDefinition(info =
@Info(title = "AUTH API", version = "1.0", description = "AUTH API v1.0")
)
public class AuthserviceApplication {

	/**
	 * Main method to run the Spring Boot application.
	 *
	 * @param args Command-line arguments passed during the application startup.
	 */
	public static void main(String[] args) {
		SpringApplication.run(AuthserviceApplication.class, args);
	}


	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new io.swagger.v3.oas.models.info.Info()
						.title("CRAFT PILOT")
						.version("1.0")
						.description("CRAFT PILOT API")
						.license(new License().name("Apache 2.0").url("http://springdoc.org"))
				);
	}
}
