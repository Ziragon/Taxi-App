package com.example.userservice.integration;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @MockitoBean
    protected RabbitTemplate rabbitTemplate;
}