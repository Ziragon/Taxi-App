package com.example.tripservice;

import com.example.tripservice.client.OsrmClient;
import com.example.tripservice.client.WeatherAPIClient;
import com.example.tripservice.config.TestContainersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Objects;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @MockitoBean
    protected OsrmClient osrmClient;

    @MockitoBean
    protected WeatherAPIClient weatherClient;

    @Autowired
    protected CacheManager cacheManager;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    protected void clearCaches() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });

        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }
}