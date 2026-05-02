package com.example.tripservice;

import com.example.tripservice.client.OsrmClient;
import com.example.tripservice.client.WeatherAPIClient;
import com.example.tripservice.config.TestContainersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    @Autowired
    private RedisTemplate<String, Long> longRedisTemplate;

    protected void clearKeysByPattern(String pattern) {
        var keys = longRedisTemplate.keys(pattern + "*");
        if (keys != null && !keys.isEmpty()) {
            longRedisTemplate.delete(keys);
        }
    }

    protected void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}