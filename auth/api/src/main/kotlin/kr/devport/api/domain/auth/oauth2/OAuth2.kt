package kr.devport.api.domain.auth.oauth2

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.enums.AuthProvider
import kr.devport.api.domain.auth.enums.UserRole
import kr.devport.api.domain.auth.infrastructure.CaptchaVerifier
import kr.devport.api.domain.auth.infrastructure.UserRepository
import kr.devport.api.domain.auth.security.CustomUserDetailsFactory
import kr.devport.api.domain.auth.service.OAuth2ExchangeCodeService
import kr.devport.api.domain.common.logging.LogSanitizer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64

abstract class OAuth2UserInfo(protected val attributes: Map<String, Any>) {
    abstract val id: String?
    abstract val name: String?
    abstract val email: String?
    abstract val imageUrl: String?
}

class GitHubOAuth2UserInfo(attributes: Map<String, Any>) : OAuth2UserInfo(attributes) {
    override val id: String? get() = attributes["id"]?.toString()
    override val name: String? get() = attributes["name"] as? String
    override val email: String? get() = attributes["email"] as? String
    override val imageUrl: String? get() = attributes["avatar_url"] as? String
}

class GoogleOAuth2UserInfo(attributes: Map<String, Any>) : OAuth2UserInfo(attributes) {
    override val id: String? get() = attributes["sub"] as? String
    override val name: String? get() = attributes["name"] as? String
    override val email: String? get() = attributes["email"] as? String
    override val imageUrl: String? get() = attributes["picture"] as? String
}

class NaverOAuth2UserInfo(attributes: Map<String, Any>) : OAuth2UserInfo(attributes) {
    @Suppress("UNCHECKED_CAST")
    private fun response(): Map<String, Any>? = attributes["response"] as? Map<String, Any>

    override val id: String? get() = response()?.get("id")?.toString()
    override val name: String? get() = response()?.get("name") as? String
    override val email: String? get() = response()?.get("email") as? String
    override val imageUrl: String? get() = response()?.get("profile_image") as? String
}

object OAuth2UserInfoFactory {
    fun getOAuth2UserInfo(
        registrationId: String,
        attributes: Map<String, Any>,
    ): OAuth2UserInfo =
        when {
            registrationId.equals(AuthProvider.google.name, ignoreCase = true) -> GoogleOAuth2UserInfo(attributes)
            registrationId.equals(AuthProvider.github.name, ignoreCase = true) -> GitHubOAuth2UserInfo(attributes)
            registrationId.equals(AuthProvider.naver.name, ignoreCase = true) -> NaverOAuth2UserInfo(attributes)
            else -> throw IllegalArgumentException("Login with $registrationId is not supported")
        }
}

/**
 * Appends Turnstile token / intent / terms version to the OAuth2 state parameter.
 * state format: originalState~base64Url(JSON{"t","i","v"})
 */
class CustomOAuth2AuthorizationRequestResolver(
    repo: ClientRegistrationRepository,
) : OAuth2AuthorizationRequestResolver {
    private val defaultResolver = DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization")

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? = customize(defaultResolver.resolve(request), request)

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? = customize(defaultResolver.resolve(request, clientRegistrationId), request)

    private fun customize(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
    ): OAuth2AuthorizationRequest? {
        if (authorizationRequest == null) return null

        var turnstileToken = request.getParameter("turnstile_token")
        if (turnstileToken.isNullOrEmpty()) {
            turnstileToken = request.cookies?.firstOrNull { it.name == TURNSTILE_COOKIE_NAME }?.value
        }
        val intent = request.getParameter("intent")
        val agreedTermsVersion = request.getParameter("agreed_terms_version")

        if (turnstileToken.isNullOrEmpty()) {
            log.debug("Turnstile token not found during OAuth2 authorization request")
            return authorizationRequest
        }

        return try {
            val payload = OBJECT_MAPPER.createObjectNode()
            payload.put("t", turnstileToken)
            if (!intent.isNullOrEmpty()) payload.put("i", intent)
            if (!agreedTermsVersion.isNullOrEmpty()) payload.put("v", agreedTermsVersion)
            val json = OBJECT_MAPPER.writeValueAsString(payload)
            val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
            val newState = authorizationRequest.state + STATE_DELIMITER + encoded
            OAuth2AuthorizationRequest.from(authorizationRequest).state(newState).build()
        } catch (e: Exception) {
            log.warn("Failed to encode OAuth2 state payload", e)
            authorizationRequest
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CustomOAuth2AuthorizationRequestResolver::class.java)
        private const val TURNSTILE_COOKIE_NAME = "turnstile_token"
        private const val STATE_DELIMITER = "~"
        private val OBJECT_MAPPER = ObjectMapper()

        private fun decodePayload(state: String?): JsonNode? {
            if (state == null || !state.contains(STATE_DELIMITER)) return null
            return try {
                val parts = state.split(STATE_DELIMITER.toRegex(), 2)
                if (parts.size < 2) {
                    null
                } else {
                    OBJECT_MAPPER.readTree(String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8))
                }
            } catch (e: Exception) {
                log.warn("Failed to decode OAuth2 state payload", e)
                null
            }
        }

        fun extractTurnstileTokenFromState(state: String?): String? = decodePayload(state)?.takeIf { it.has("t") }?.get("t")?.asText()

        fun extractIntentFromState(state: String?): String? = decodePayload(state)?.takeIf { it.has("i") }?.get("i")?.asText()

        fun extractTermsVersionFromState(state: String?): String? = decodePayload(state)?.takeIf { it.has("v") }?.get("v")?.asText()
    }
}

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : DefaultOAuth2UserService() {
    private val log = LoggerFactory.getLogger(CustomOAuth2UserService::class.java)

    @Value("\${app.auth.current-terms-version}")
    private lateinit var currentTermsVersion: String

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        var oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId

        if ("github" == registrationId && oAuth2User.getAttribute<Any?>("email") == null) {
            val email = fetchGitHubEmail(userRequest.accessToken.tokenValue)
            if (email != null) {
                val modified = HashMap(oAuth2User.attributes)
                modified["email"] = email
                oAuth2User = DefaultOAuth2User(oAuth2User.authorities, modified, "id")
            }
        }
        return processOAuth2User(userRequest, oAuth2User)
    }

    private fun fetchGitHubEmail(accessToken: String): String? =
        try {
            val headers = HttpHeaders().apply { setBearerAuth(accessToken) }
            val response =
                RestTemplate().exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    HttpEntity<Void>(headers),
                    object : ParameterizedTypeReference<List<Map<String, Any>>>() {},
                )
            val emails = response.body
            emails?.firstOrNull { it["primary"] == true }?.get("email") as? String
                ?: emails?.firstOrNull()?.get("email") as? String
        } catch (e: Exception) {
            log.warn("Failed to fetch GitHub email for OAuth2 login", e)
            null
        }

    private fun processOAuth2User(
        userRequest: OAuth2UserRequest,
        oAuth2User: OAuth2User,
    ): OAuth2User {
        val registrationId = userRequest.clientRegistration.registrationId
        val info = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.attributes)

        if (info.email.isNullOrEmpty()) {
            throw OAuth2AuthenticationException("Email not found from OAuth2 provider")
        }

        val authProvider = AuthProvider.valueOf(registrationId)
        val existing = userRepository.findByAuthProviderAndProviderId(authProvider, info.id!!)

        val user: User
        if (existing.isPresent) {
            user = updateExistingUser(existing.get(), info)
        } else {
            if (userRepository.findByEmail(info.email!!).isPresent) {
                throw OAuth2AuthenticationException("같은 정보의 계정이 이미 존재합니다")
            }
            val state = stateFromRequest()
            if ("signup" != CustomOAuth2AuthorizationRequestResolver.extractIntentFromState(state)) {
                throw OAuth2AuthenticationException("signup_required")
            }
            val agreedTermsVersion = CustomOAuth2AuthorizationRequestResolver.extractTermsVersionFromState(state)
            if (agreedTermsVersion == null || currentTermsVersion != agreedTermsVersion) {
                throw OAuth2AuthenticationException("약관 동의가 필요합니다")
            }
            user = registerNewUser(authProvider, info)
        }

        return CustomUserDetailsFactory.create(user).withAttributes(oAuth2User.attributes)
    }

    private fun registerNewUser(
        authProvider: AuthProvider,
        info: OAuth2UserInfo,
    ): User {
        val now = LocalDateTime.now()
        val user =
            User().apply {
                email = info.email
                name = info.name
                profileImageUrl = info.imageUrl
                this.authProvider = authProvider
                providerId = info.id
                role = UserRole.USER
                createdAt = now
                updatedAt = now
                lastLoginAt = now
                agreedTermsVersion = currentTermsVersion
                agreedAt = now
            }
        return userRepository.save(user)
    }

    private fun updateExistingUser(
        existing: User,
        info: OAuth2UserInfo,
    ): User {
        existing.name = info.name
        existing.profileImageUrl = info.imageUrl
        existing.updatedAt = LocalDateTime.now()
        existing.lastLoginAt = LocalDateTime.now()
        return userRepository.save(existing)
    }

    private fun stateFromRequest(): String? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return null
        return attrs.request.getParameter("state")
    }
}

@Component
class OAuth2AuthenticationSuccessHandler(
    private val oAuth2ExchangeCodeService: OAuth2ExchangeCodeService,
    private val userRepository: UserRepository,
    private val captchaVerifier: CaptchaVerifier,
) : SimpleUrlAuthenticationSuccessHandler() {
    private val log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler::class.java)

    @Value("\${app.oauth2.redirect-uri}")
    private lateinit var redirectUri: String

    @Value("\${app.oauth2.failure-redirect-uri}")
    private lateinit var failureRedirectUri: String

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val targetUrl = determineTargetUrl(request, authentication)
        if (response.isCommitted) {
            return
        }
        clearAuthenticationAttributes(request)
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }

    private fun determineTargetUrl(
        request: HttpServletRequest,
        authentication: Authentication,
    ): String {
        val state = request.getParameter("state")
        val turnstileToken = CustomOAuth2AuthorizationRequestResolver.extractTurnstileTokenFromState(state)
        if (turnstileToken.isNullOrEmpty()) {
            return buildFailureRedirectUrl("Turnstile token is missing")
        }
        val clientIp = getClientIp(request)
        if (!captchaVerifier.verify(turnstileToken, clientIp)) {
            log.warn("Turnstile validation failed for OAuth2 login, clientIp={}", LogSanitizer.maskIp(clientIp))
            return buildFailureRedirectUrl("Bot verification failed")
        }
        val userDetails = authentication.principal as kr.devport.api.domain.common.security.CustomUserDetails
        val user = userRepository.findById(userDetails.id).orElseThrow { RuntimeException("User not found") }
        val exchangeCode = oAuth2ExchangeCodeService.createExchangeCode(user, request.getHeader("User-Agent"))
        return UriComponentsBuilder.fromUriString(redirectUri).queryParam("code", exchangeCode).build().toUriString()
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val headers = listOf("X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR")
        var ip: String? = null
        for (h in headers) {
            val value = request.getHeader(h)
            if (!value.isNullOrEmpty() && !"unknown".equals(value, ignoreCase = true)) {
                ip = value
                break
            }
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim()
        }
        return ip ?: ""
    }

    private fun buildFailureRedirectUrl(errorMessage: String): String =
        UriComponentsBuilder.fromUriString(failureRedirectUri).queryParam("error", errorMessage).build().toUriString()
}

@Component
class OAuth2AuthenticationFailureHandler : SimpleUrlAuthenticationFailureHandler() {
    @Value("\${app.oauth2.redirect-uri}")
    private lateinit var redirectUri: String

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        var errorMessage = exception.localizedMessage
        if (errorMessage.isNullOrBlank()) errorMessage = exception.message
        if (errorMessage.isNullOrBlank() && exception is OAuth2AuthenticationException) {
            errorMessage = exception.error.errorCode
        }
        if (errorMessage.isNullOrBlank()) errorMessage = exception.javaClass.simpleName
        val targetUrl =
            UriComponentsBuilder.fromUriString(redirectUri).queryParam("error", errorMessage).build().toUriString()
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
