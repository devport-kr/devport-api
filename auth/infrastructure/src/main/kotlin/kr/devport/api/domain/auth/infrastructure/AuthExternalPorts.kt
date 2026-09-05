package kr.devport.api.domain.auth.infrastructure

/**
 * Out-port: short-lived OAuth2 exchange-code store (a Redis adapter implements it).
 * The core owns the payload shape and TTL policy; the adapter owns the storage detail.
 */
interface OAuth2ExchangeCodeStore {
    fun put(
        code: String,
        payload: ExchangeCodePayload,
        ttlSeconds: Long,
    )

    /** Atomically reads and removes the payload for [code], or null if absent/expired. */
    fun take(code: String): ExchangeCodePayload?
}

data class ExchangeCodePayload(
    val userId: Long,
    val userAgentHash: String,
)

/**
 * Out-port: bot/captcha verification (an HTTP adapter implements it, e.g. Cloudflare Turnstile).
 */
interface CaptchaVerifier {
    fun verify(
        token: String?,
        remoteIp: String?,
    ): Boolean
}
