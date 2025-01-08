package com.craftpilot.productservice.base;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractTestContainerConfiguration {

    static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void beforeAll() {
        POSTGRESQL_CONTAINER.withReuse(true);
        POSTGRESQL_CONTAINER.start();
    }

    @DynamicPropertySource
    private static void overrideProps(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
    }
}
