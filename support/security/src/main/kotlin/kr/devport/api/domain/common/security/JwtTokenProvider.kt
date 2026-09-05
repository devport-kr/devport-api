package kr.devport.api.domain.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    @param:Value("\${app.jwt.secret}") private val jwtSecret: String,
    @param:Value("\${app.jwt.access-token-expiration-ms}") val accessTokenExpirationMs: Long,
    @param:Value("\${app.jwt.refresh-token-expiration-ms}") val refreshTokenExpirationMs: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateAccessToken(userId: Long?): String = generateToken(userId, accessTokenExpirationMs)

    fun createAccessToken(userId: Long?): String = generateAccessToken(userId)

    fun generateRefreshToken(userId: Long?): String = generateToken(userId, refreshTokenExpirationMs)

    fun getUserIdFromToken(token: String?): Long {
        val claims = parseAndValidateClaims(token)
        val subject = claims["sub"]
        if (subject !is String || subject.isBlank()) {
            throw IllegalArgumentException("JWT subject is missing")
        }
        return subject.toLong()
    }

    fun validateToken(token: String?): Boolean {
        try {
            parseAndValidateClaims(token)
            return true
        } catch (ex: IllegalArgumentException) {
            log.debug("Rejected JWT token: {}", ex.message)
        }
        return false
    }

    private fun generateToken(
        userId: Long?,
        expirationMs: Long,
    ): String {
        val issuedAtSeconds = System.currentTimeMillis() / 1000
        val expirationSeconds = issuedAtSeconds + (expirationMs / 1000)

        val header =
            linkedMapOf<String, Any>(
                "alg" to "HS512",
                "typ" to "JWT",
            )
        val payload =
            linkedMapOf<String, Any>(
                "sub" to userId.toString(),
                "iat" to issuedAtSeconds,
                "exp" to expirationSeconds,
            )

        val encodedHeader = encodeJson(header)
        val encodedPayload = encodeJson(payload)
        val signature = sign("$encodedHeader.$encodedPayload")
        return "$encodedHeader.$encodedPayload.$signature"
    }

    private fun parseAndValidateClaims(token: String?): Map<String, Any> {
        if (token.isNullOrBlank()) {
            throw IllegalArgumentException("JWT token is empty")
        }

        val parts = token.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT token")
        }

        val header = decodeJson(parts[0])
        if (header["alg"] != "HS512") {
            throw IllegalArgumentException("Unsupported JWT token")
        }

        val expectedSignature = sign("${parts[0]}.${parts[1]}")
        if (!MessageDigest.isEqual(
                expectedSignature.toByteArray(StandardCharsets.UTF_8),
                parts[2].toByteArray(StandardCharsets.UTF_8),
            )
        ) {
            throw IllegalArgumentException("Invalid JWT token")
        }

        val claims = decodeJson(parts[1])
        val expiration = claims["exp"]
        if (expiration !is Number) {
            throw IllegalArgumentException("Invalid JWT token")
        }

        val nowSeconds = System.currentTimeMillis() / 1000
        if (expiration.toLong() <= nowSeconds) {
            throw IllegalArgumentException("Expired JWT token")
        }

        return claims
    }

    private fun encodeJson(value: Map<String, Any>): String =
        try {
            BASE64_URL_ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(value))
        } catch (e: Exception) {
            throw IllegalStateException("Failed to serialize JWT payload", e)
        }

    @Suppress("UNCHECKED_CAST")
    private fun decodeJson(encodedValue: String): Map<String, Any> =
        try {
            val decoded = BASE64_URL_DECODER.decode(encodedValue)
            OBJECT_MAPPER.readValue(decoded, Map::class.java) as Map<String, Any>
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JWT token", e)
        }

    private fun sign(content: String): String =
        try {
            val mac = Mac.getInstance(HMAC_SHA512)
            val keySpec = SecretKeySpec(jwtSecret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA512)
            mac.init(keySpec)
            BASE64_URL_ENCODER.encodeToString(mac.doFinal(content.toByteArray(StandardCharsets.UTF_8)))
        } catch (e: Exception) {
            throw IllegalStateException("Failed to sign JWT token", e)
        }

    companion object {
        private val BASE64_URL_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        private val BASE64_URL_DECODER: Base64.Decoder = Base64.getUrlDecoder()
        private const val HMAC_SHA512 = "HmacSHA512"
        private val OBJECT_MAPPER = ObjectMapper()
    }
}
