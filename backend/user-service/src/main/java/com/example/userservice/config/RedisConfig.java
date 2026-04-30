package com.example.userservice.config;

import com.example.shared.dto.data.DriverLocationDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, DriverLocationDto> redisTemplate(
            RedisConnectionFactory factory) {

        RedisTemplate<String, DriverLocationDto> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());

        JacksonJsonRedisSerializer<DriverLocationDto> serializer =
                new JacksonJsonRedisSerializer<>(DriverLocationDto.class);

        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
