package kr.devport.api.domain.auth.adapter.http

import kr.devport.api.domain.auth.infrastructure.CaptchaVerifier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

/** HTTP adapter for [CaptchaVerifier] backed by Cloudflare Turnstile siteverify. */
@Component
class TurnstileCaptchaVerifier : CaptchaVerifier {
    private val log = LoggerFactory.getLogger(TurnstileCaptchaVerifier::class.java)
    private val restTemplate = RestTemplate()

    @Value("\${cloudflare.turnstile.secret-key}")
    private lateinit var secretKey: String

    override fun verify(
        token: String?,
        remoteIp: String?,
    ): Boolean {
        if (token.isNullOrBlank()) return false
        return try {
            val body = LinkedMultiValueMap<String, String>()
            body.add("secret", secretKey)
            body.add("response", token)
            if (!remoteIp.isNullOrEmpty()) body.add("remoteip", remoteIp)
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_FORM_URLENCODED }
            val response =
                restTemplate.exchange(
                    TURNSTILE_VERIFY_URL,
                    HttpMethod.POST,
                    HttpEntity(body, headers),
                    Map::class.java,
                )
            response.body?.get("success") == true
        } catch (e: Exception) {
            log.error("Error during Turnstile token validation", e)
            false
        }
    }

    companion object {
        private const val TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify"
    }
}
