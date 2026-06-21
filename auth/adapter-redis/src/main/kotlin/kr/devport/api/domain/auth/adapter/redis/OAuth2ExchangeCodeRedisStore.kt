package kr.devport.api.domain.auth.adapter.redis

import kr.devport.api.domain.auth.infrastructure.ExchangeCodePayload
import kr.devport.api.domain.auth.infrastructure.OAuth2ExchangeCodeStore
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis adapter for [OAuth2ExchangeCodeStore]. The persisted Map shape ({userId, userAgentHash})
 * and key prefix are kept identical so in-flight codes survive a deploy.
 */
@Component
class OAuth2ExchangeCodeRedisStore(
    private val redisTemplate: RedisTemplate<String, Any>,
) : OAuth2ExchangeCodeStore {
    override fun put(
        code: String,
        payload: ExchangeCodePayload,
        ttlSeconds: Long,
    ) {
        val stored =
            mapOf<String, Any>(
                USER_ID_KEY to payload.userId,
                USER_AGENT_HASH_KEY to payload.userAgentHash,
            )
        redisTemplate.opsForValue().set(buildKey(code), stored, Duration.ofSeconds(ttlSeconds))
    }

    override fun take(code: String): ExchangeCodePayload? {
        val stored = redisTemplate.opsForValue().getAndDelete(buildKey(code))
        if (stored !is Map<*, *>) return null
        val userId = (stored[USER_ID_KEY] as? Number)?.toLong() ?: return null
        val userAgentHash = stored[USER_AGENT_HASH_KEY] as? String ?: return null
        return ExchangeCodePayload(userId, userAgentHash)
    }

    private fun buildKey(code: String) = KEY_PREFIX + code

    companion object {
        private const val KEY_PREFIX = "auth:oauth2:exchange:"
        private const val USER_ID_KEY = "userId"
        private const val USER_AGENT_HASH_KEY = "userAgentHash"
    }
}
