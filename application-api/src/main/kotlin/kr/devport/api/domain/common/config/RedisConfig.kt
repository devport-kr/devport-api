package kr.devport.api.domain.common.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
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

        // SCAN-based batch strategy for production-safe cache clearing (avoids the blocking KEYS command).
        val cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(1000))

        return RedisCacheManager
            .builder(cacheWriter)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    internal fun jsonRedisSerializer(): RedisSerializer<Any> {
        val objectMapper = ObjectMapper()

        // Kotlin module: DTOs are final data classes, many without a no-arg constructor.
        objectMapper.registerModule(KotlinModule.Builder().build())
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        // Type info for EVERYTHING, including final classes. NON_FINAL skips Kotlin data classes (final by
        // default), so the value is written without "@class" and can't be read back as Any -> every cache
        // hit failed with "missing type id property '@class'".
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType(Any::class.java)
                .build(),
            ObjectMapper.DefaultTyping.EVERYTHING,
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
