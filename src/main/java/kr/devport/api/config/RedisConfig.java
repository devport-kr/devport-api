package kr.devport.api.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching
 * Configures cache managers with different TTLs for different cache types
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Configure Redis template for custom operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(jsonRedisSerializer());
        template.setHashValueSerializer(jsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure cache manager with different TTLs for different caches
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration (5 minutes)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer()))
            .disableCachingNullValues();

        // Custom configurations for different cache types
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Trending ticker - 5 minutes (frequently updated)
        cacheConfigurations.put("trendingTicker", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // GitHub trending - 1 hour (updated less frequently)
        cacheConfigurations.put("githubTrending", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Articles by category - 10 minutes
        cacheConfigurations.put("articles", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Git repos - 10 minutes
        cacheConfigurations.put("gitRepos", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Trending git repos - 1 hour
        cacheConfigurations.put("trendingGitRepos", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Git repos by language - 30 minutes
        cacheConfigurations.put("gitReposByLanguage", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // LLM rankings - 24 hours (rarely changes)
        cacheConfigurations.put("llmRankings", defaultConfig.entryTtl(Duration.ofHours(24)));

        // All benchmarks - 24 hours (static data)
        cacheConfigurations.put("benchmarks", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    /**
     * JSON serializer for Redis values
     * Configured to handle DTOs without Hibernate proxy issues
     */
    private GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register Java 8 date/time module for LocalDateTime support
        objectMapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Ignore unknown properties during deserialization
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Enable default typing with stricter validation to avoid Hibernate proxy issues
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
