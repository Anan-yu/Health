package com.rayk.health.integration;

import com.rayk.health.integration.ai.AiServiceClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 集成测试配置：提供外部依赖的 Mock Bean，使测试仅需 H2 数据库即可运行。
 * MinioClient 和 WebClient 是懒加载的，启动时不会连接外部服务，无需 Mock。
 */
@TestConfiguration
class TestExternalServicesConfig {
    @Bean
    @Primary
    RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    AiServiceClient aiServiceClient() {
        return Mockito.mock(AiServiceClient.class);
    }
}
