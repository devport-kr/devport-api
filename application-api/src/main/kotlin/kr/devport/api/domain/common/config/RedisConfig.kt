package kr.devport.api.domain.common.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kr.devport.api.domain.article.dto.response.ArticlePageResponse
import kr.devport.api.domain.common.cache.CacheNames
import kr.devport.api.domain.common.cache.CacheTtlPolicy
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.BatchStrategies
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableCaching
class RedisConfig {
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()

        template.valueSerializer = jsonRedisSerializer()
        template.hashValueSerializer = jsonRedisSerializer()

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val defaultConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(CacheTtlPolicy.DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer()))
                .disableCachingNullValues()

        // Build cache configurations from the centralized TTL policy.
        val cacheConfigurations = HashMap<String, RedisCacheConfiguration>()
        CacheTtlPolicy.getAllTtls().forEach { (cacheName, ttl) ->
            cacheConfigurations[cacheName] = defaultConfig.entryTtl(ttl)
        }

        // Search results are keyed by user input, so use an explicitly typed serializer (no polymorphic
        // typing) rather than relying on default typing to round-trip a final Kotlin data class.
        cacheConfigurations[CacheNames.ARTICLE_SEARCH] =
            defaultConfig
                .entryTtl(CacheTtlPolicy.getTtl(CacheNames.ARTICLE_SEARCH))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        typedJsonSerializer(ArticlePageResponse::class.java),
                    ),
                )

        // SCAN-based batch strategy for production-safe cache clearing (avoids the blocking KEYS command).
        val cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(1000))

        return RedisCacheManager
            .builder(cacheWriter)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    private fun <T : Any> typedJsonSerializer(type: Class<T>): RedisSerializer<Any> {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        return object : RedisSerializer<Any> {
            override fun serialize(value: Any?): ByteArray =
                if (value == null) {
                    ByteArray(0)
                } else {
                    try {
                        objectMapper.writeValueAsBytes(value)
                    } catch (e: Exception) {
                        throw SerializationException("Could not serialize: ${e.message}", e)
                    }
                }

            override fun deserialize(bytes: ByteArray?): Any? =
                if (bytes == null || bytes.isEmpty()) {
                    null
                } else {
                    try {
                        objectMapper.readValue(bytes, type)
                    } catch (e: Exception) {
                        throw SerializationException("Could not deserialize: ${e.message}", e)
                    }
                }
        }
    }

    private fun jsonRedisSerializer(): RedisSerializer<Any> {
        val objectMapper = ObjectMapper()

        objectMapper.registerModule(JavaTimeModule())
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        // Include type information so Hibernate proxies / polymorphic values round-trip through Redis.
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType(Any::class.java)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY,
        )

        return object : RedisSerializer<Any> {
            override fun serialize(value: Any?): ByteArray {
                if (value == null) {
                    return ByteArray(0)
                }
                return try {
                    objectMapper.writeValueAsBytes(value)
                } catch (e: Exception) {
                    throw SerializationException("Could not serialize: ${e.message}", e)
                }
            }

            override fun deserialize(bytes: ByteArray?): Any? {
                if (bytes == null || bytes.isEmpty()) {
                    return null
                }
                return try {
                    objectMapper.readValue(bytes, Any::class.java)
                } catch (e: Exception) {
                    throw SerializationException("Could not deserialize: ${e.message}", e)
                }
            }
        }
    }
}
