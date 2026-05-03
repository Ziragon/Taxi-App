package com.example.tripservice.config;

import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.data.WeatherDto;
import com.example.tripservice.service.search.DriverResponseSubscriber;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Long> longRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        return template;
    }

    // Адаптер вызова метода onMessage
    @Bean
    public MessageListenerAdapter driverResponseListenerAdapter(DriverResponseSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    // Открытие соединения с redis
    // Контейнер подписывается на паттерн "driver:response:*"
    @Bean
    public RedisMessageListenerContainer redisListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter driverResponseListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(driverResponseListenerAdapter, new PatternTopic("driver:response:*"));
        return container;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        JacksonJsonRedisSerializer<WeatherDto> weatherSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, WeatherDto.class);

        JacksonJsonRedisSerializer<RouteDto> routeSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, RouteDto.class);

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                );

        RedisCacheConfiguration weatherConfig = baseConfig
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(weatherSerializer)
                )
                .entryTtl(Duration.ofMinutes(10));

        RedisCacheConfiguration routeConfig = baseConfig
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(routeSerializer)
                )
                .entryTtl(Duration.ofMinutes(15));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("weather", weatherConfig)
                .withCacheConfiguration("routes", routeConfig)
                .build();
    }
}