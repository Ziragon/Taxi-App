package com.example.userservice.config;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

public class TestContainersConfig {

    private static final PostgreSQLContainer<?> postgresContainer;
    private static final RabbitMQContainer rabbitMQContainer;

    static {
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                .withReuse(true);
        postgresContainer.start();

        rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.0-management-alpine"))
                .withReuse(true);
        rabbitMQContainer.start();
    }

    public static PostgreSQLContainer<?> getPostgresContainer() {
        return postgresContainer;
    }

    public static RabbitMQContainer getRabbitMQContainer() {
        return rabbitMQContainer;
    }
}