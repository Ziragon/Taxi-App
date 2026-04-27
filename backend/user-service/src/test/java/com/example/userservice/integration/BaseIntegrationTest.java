package com.example.userservice.integration;

import com.example.userservice.config.TestContainersConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", TestContainersConfig.getPostgresContainer()::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainersConfig.getPostgresContainer()::getUsername);
        registry.add("spring.datasource.password", TestContainersConfig.getPostgresContainer()::getPassword);

        registry.add("spring.rabbitmq.host", TestContainersConfig.getRabbitMQContainer()::getHost);
        registry.add("spring.rabbitmq.port", TestContainersConfig.getRabbitMQContainer()::getAmqpPort);
        registry.add("spring.rabbitmq.username", TestContainersConfig.getRabbitMQContainer()::getAdminUsername);
        registry.add("spring.rabbitmq.password", TestContainersConfig.getRabbitMQContainer()::getAdminPassword);
    }
}