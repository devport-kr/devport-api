package kr.devport.api.domain.auth.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.devport.api.domain.auth.dto.AuthResponse
import kr.devport.api.domain.auth.dto.LoginRequest
import kr.devport.api.domain.auth.dto.SignupRequest
import kr.devport.api.domain.auth.dto.SignupResponse
import kr.devport.api.domain.auth.dto.TokenResponse
import kr.devport.api.domain.auth.dto.UserResponse
import kr.devport.api.domain.auth.entity.EmailVerificationToken
import kr.devport.api.domain.auth.entity.PasswordResetToken
import kr.devport.api.domain.auth.entity.RefreshToken
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.enums.AuthProvider
import kr.devport.api.domain.auth.enums.UserRole
import kr.devport.api.domain.auth.repository.EmailVerificationTokenRepository
import kr.devport.api.domain.auth.repository.PasswordResetTokenRepository
import kr.devport.api.domain.auth.repository.RefreshTokenRepository
import kr.devport.api.domain.auth.repository.UserRepository
import kr.devport.api.domain.common.exception.DuplicateEmailException
import kr.devport.api.domain.common.exception.DuplicateUsernameException
import kr.devport.api.domain.common.exception.EmailVerificationRequiredException
import kr.devport.api.domain.common.exception.InvalidCredentialsException
import kr.devport.api.domain.common.exception.InvalidTermsAgreementException
import kr.devport.api.domain.common.exception.InvalidTokenException
import kr.devport.api.domain.common.exception.OAuth2AccountException
import kr.devport.api.domain.common.exception.TokenExpiredException
import kr.devport.api.domain.common.exception.TokenNotFoundException
import kr.devport.api.domain.common.logging.LogSanitizer
import kr.devport.api.domain.common.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64
import java.util.Optional
import java.util.UUID

fun User.toUserResponse(): UserResponse =
    UserResponse(
        id = id,
        email = email,
        username = username,
        emailVerified = emailVerified,
        name = name,
        profileImageUrl = profileImageUrl,
        authProvider = authProvider,
        role = role,
        createdAt = createdAt,
        lastLoginAt = lastLoginAt,
        flair = flair,
        flairColor = flairColor,
    )

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenService: RefreshTokenService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val oAuth2ExchangeCodeService: OAuth2ExchangeCodeService,
) {
    fun getCurrentUser(userId: Long): UserResponse =
        userRepository.findById(userId).orElseThrow { RuntimeException("User not found with id: $userId") }.toUserResponse()

    @Transactional
    fun refreshAccessToken(refreshTokenString: String): TokenResponse {
        val refreshToken = refreshTokenService.requireValidRefreshToken(refreshTokenString)
        val user = refreshToken.user!!
        val newAccessToken = jwtTokenProvider.generateAccessToken(user.id)
        val rotated = refreshTokenService.rotateRefreshToken(refreshToken)
        return TokenResponse(newAccessToken, rotated, "Bearer", jwtTokenProvider.accessTokenExpirationMs / 1000)
    }

    @Transactional
    fun logout(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found with id: $userId") }
        refreshTokenService.deleteByUser(user)
    }

    @Transactional
    fun exchangeOAuth2Code(
        code: String,
        request: HttpServletRequest,
    ): TokenResponse {
        val user = oAuth2ExchangeCodeService.consumeExchangeCode(code, request)
        val accessToken = jwtTokenProvider.generateAccessToken(user.id)
        val refreshToken = refreshTokenService.createRefreshToken(user)
        return TokenResponse(accessToken, refreshToken, "Bearer", jwtTokenProvider.accessTokenExpirationMs / 1000)
    }
}

@Service
class LoginService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {
    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user =
            userRepository.findByUsername(request.username).orElseThrow {
                InvalidCredentialsException("Invalid username or password")
            }
        if (user.authProvider != AuthProvider.local) throw InvalidCredentialsException("Invalid username or password")
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException("Invalid username or password")
        }
        if (!user.emailVerified) throw EmailVerificationRequiredException("Email verification is required before login")

        user.lastLoginAt = LocalDateTime.now()
        userRepository.save(user)

        val accessToken = jwtTokenProvider.createAccessToken(user.id)
        val refreshToken = refreshTokenService.createRefreshToken(user)
        return AuthResponse(accessToken, refreshToken, "Bearer", jwtTokenProvider.accessTokenExpirationMs / 1000)
    }
}

@Service
class SignupService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
) {
    private val log = LoggerFactory.getLogger(SignupService::class.java)

    @Value("\${app.auth.current-terms-version}")
    private lateinit var currentTermsVersion: String

    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        validateTermsAgreement(request.agreedTermsVersion)
        if (userRepository.existsByUsername(request.username)) {
            throw DuplicateUsernameException("Username already exists: ${request.username}")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEmailException("Email already exists: ${request.email}")
        }
        val now = LocalDateTime.now()
        val user =
            User().apply {
                username = request.username
                password = passwordEncoder.encode(request.password)
                email = request.email
                name = request.name
                authProvider = AuthProvider.local
                role = UserRole.USER
                emailVerified = false
                emailAddedAt = now
                createdAt = now
                updatedAt = now
                agreedTermsVersion = request.agreedTermsVersion
                agreedAt = now
            }
        val saved = userRepository.save(user)
        log.info("User signup completed for userId={}", saved.id)

        val token = UUID.randomUUID().toString()
        verificationTokenRepository.save(
            EmailVerificationToken().apply {
                this.token = token
                this.user = saved
                expiresAt = now.plusHours(24)
                createdAt = now
            },
        )
        emailService.sendVerificationEmail(saved, token)
        return SignupResponse("Account created. Verify your email before logging in.", true, saved.email)
    }

    private fun validateTermsAgreement(agreedTermsVersion: String) {
        try {
            LocalDate.parse(agreedTermsVersion)
        } catch (ex: Exception) {
            throw InvalidTermsAgreementException("Terms version must be a valid date in YYYY-MM-DD format")
        }
        if (currentTermsVersion != agreedTermsVersion) {
            throw InvalidTermsAgreementException("You must agree to the current terms version to sign up")
        }
    }

    fun isUsernameAvailable(username: String): Boolean = !userRepository.existsByUsername(username)

    fun isEmailAvailable(email: String): Boolean = !userRepository.existsByEmail(email)
}

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    private val secureRandom = SecureRandom()

    @Value("\${app.jwt.refresh-token-expiration-ms}")
    private var refreshTokenExpirationMs: Long = 0

    @Transactional
    fun createRefreshToken(user: User): String {
        refreshTokenRepository.deleteByUser(user)
        return issueRefreshToken(user)
    }

    @Transactional
    fun requireValidRefreshToken(rawToken: String): RefreshToken {
        val refreshToken =
            findByRawToken(rawToken).orElseThrow { InvalidTokenException("Refresh token is invalid or expired") }
        if (refreshToken.isValid()) return refreshToken
        refreshTokenRepository.delete(refreshToken)
        throw InvalidTokenException("Refresh token is invalid or expired")
    }

    @Transactional
    fun rotateRefreshToken(refreshToken: RefreshToken): String {
        if (!refreshToken.isValid()) {
            refreshTokenRepository.delete(refreshToken)
            throw InvalidTokenException("Refresh token is invalid or expired")
        }
        val user = refreshToken.user!!
        refreshTokenRepository.delete(refreshToken)
        return issueRefreshToken(user)
    }

    @Transactional(readOnly = true)
    fun findByRawToken(rawToken: String): Optional<RefreshToken> = refreshTokenRepository.findByToken(hashToken(rawToken))

    @Transactional
    fun revokeRefreshToken(rawToken: String) {
        findByRawToken(rawToken).ifPresent {
            it.revoke()
            refreshTokenRepository.save(it)
        }
    }

    @Transactional
    fun deleteByUser(user: User) = refreshTokenRepository.deleteByUser(user)

    private fun issueRefreshToken(user: User): String {
        val rawToken = generateOpaqueToken()
        refreshTokenRepository.save(
            RefreshToken().apply {
                this.user = user
                token = hashToken(rawToken)
                expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000)
                createdAt = LocalDateTime.now()
            },
        )
        return rawToken
    }

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(rawToken: String): String {
        val hashed = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed)
    }
}

@Service
class RefreshTokenCookieService {
    @Value("\${app.auth.refresh-cookie-name:devport_refresh_token}")
    private lateinit var cookieName: String

    @Value("\${app.auth.refresh-cookie-path:/api/auth}")
    private lateinit var cookiePath: String

    @Value("\${app.auth.refresh-cookie-domain:}")
    private lateinit var cookieDomain: String

    @Value("\${app.auth.refresh-cookie-secure:false}")
    private var secureCookie: Boolean = false

    @Value("\${app.auth.refresh-cookie-same-site:Lax}")
    private lateinit var sameSite: String

    @Value("\${app.jwt.refresh-token-expiration-ms}")
    private var refreshTokenExpirationMs: Long = 0

    fun addRefreshTokenCookie(
        response: HttpServletResponse,
        refreshToken: String,
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshToken, Duration.ofMillis(refreshTokenExpirationMs)).toString())
    }

    fun clearRefreshTokenCookie(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString())
    }

    fun getCookieName(): String = cookieName

    private fun buildCookie(
        value: String,
        maxAge: Duration,
    ): ResponseCookie {
        val builder =
            ResponseCookie
                .from(cookieName, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(maxAge)
        if (cookieDomain.isNotBlank()) builder.domain(cookieDomain)
        return builder.build()
    }
}

@Service
class OAuth2ExchangeCodeService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val userRepository: UserRepository,
) {
    private val secureRandom = SecureRandom()

    @Value("\${app.auth.oauth2-exchange-ttl-seconds:60}")
    private var exchangeCodeTtlSeconds: Long = 60

    fun createExchangeCode(
        user: User,
        request: HttpServletRequest,
    ): String {
        val code = generateCode()
        val payload =
            mapOf<String, Any>(
                USER_ID_KEY to (user.id ?: 0L),
                USER_AGENT_HASH_KEY to hash(normalizeUserAgent(request.getHeader("User-Agent"))),
            )
        redisTemplate.opsForValue().set(buildKey(code), payload, Duration.ofSeconds(exchangeCodeTtlSeconds))
        return code
    }

    fun consumeExchangeCode(
        code: String,
        request: HttpServletRequest,
    ): User {
        val stored = redisTemplate.opsForValue().getAndDelete(buildKey(code))
        if (stored !is Map<*, *>) throw InvalidTokenException("OAuth2 exchange code is invalid or expired")
        val userId = stored[USER_ID_KEY] as? Number ?: throw InvalidTokenException("OAuth2 exchange code is invalid or expired")
        val userAgentHash =
            stored[USER_AGENT_HASH_KEY] as? String ?: throw InvalidTokenException("OAuth2 exchange code is invalid or expired")
        if (userAgentHash != hash(normalizeUserAgent(request.getHeader("User-Agent")))) {
            throw InvalidTokenException("OAuth2 exchange code is invalid or expired")
        }
        return userRepository
            .findById(userId.toLong())
            .orElseThrow { InvalidTokenException("OAuth2 exchange code is invalid or expired") }
    }

    private fun buildKey(code: String) = KEY_PREFIX + code

    private fun generateCode(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun normalizeUserAgent(userAgent: String?): String = userAgent?.trim() ?: ""

    private fun hash(value: String): String {
        val hashed = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed)
    }

    companion object {
        private const val KEY_PREFIX = "auth:oauth2:exchange:"
        private const val USER_ID_KEY = "userId"
        private const val USER_AGENT_HASH_KEY = "userAgentHash"
    }
}

@Service
class EmailService(
    private val mailSender: JavaMailSender,
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    @Value("\${app.email.from}")
    private lateinit var fromEmail: String

    @Value("\${app.email.verification-url}")
    private lateinit var verificationUrlTemplate: String

    @Value("\${app.email.reset-password-url}")
    private lateinit var resetPasswordUrlTemplate: String

    fun sendVerificationEmail(
        user: User,
        token: String,
    ) {
        try {
            val url = verificationUrlTemplate.replace("{token}", token)
            val message =
                SimpleMailMessage().apply {
                    from = fromEmail
                    setTo(user.email!!)
                    subject = "DevPort - 이메일 인증"
                    text =
                        "안녕하세요 ${user.name ?: user.username}님,\n\ndevport에 가입해주셔서 감사합니다!\n\n" +
                        "아래 링크를 클릭하여 이메일 주소를 인증해주세요:\n$url\n\n이 링크는 24시간 후에 만료됩니다.\n\n" +
                        "본인이 가입하지 않았다면 이 이메일을 무시하셔도 됩니다.\n\n감사합니다,\ndevport"
                }
            mailSender.send(message)
            log.debug("Verification email sent to {}", LogSanitizer.maskEmail(user.email))
        } catch (e: Exception) {
            log.error("Failed to send verification email to {}", LogSanitizer.maskEmail(user.email), e)
            throw RuntimeException("Failed to send verification email", e)
        }
    }

    fun sendPasswordResetEmail(
        user: User,
        token: String,
    ) {
        try {
            val url = resetPasswordUrlTemplate.replace("{token}", token)
            val message =
                SimpleMailMessage().apply {
                    from = fromEmail
                    setTo(user.email!!)
                    subject = "DevPort - 비밀번호 재설정"
                    text =
                        "안녕하세요 ${user.name ?: user.username}님,\n\n아래 링크를 클릭하여 비밀번호를 재설정해주세요:\n$url\n\n" +
                        "이 링크는 1시간 후에 만료됩니다.\n\n본인이 요청하지 않았다면 이 이메일을 무시하셔도 됩니다.\n\n감사합니다,\ndevport"
                }
            mailSender.send(message)
            log.debug("Password reset email sent to {}", LogSanitizer.maskEmail(user.email))
        } catch (e: Exception) {
            log.error("Failed to send password reset email to {}", LogSanitizer.maskEmail(user.email), e)
            throw RuntimeException("Failed to send password reset email", e)
        }
    }
}

@Service
class EmailVerificationService(
    private val tokenRepository: EmailVerificationTokenRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
) {
    private val log = LoggerFactory.getLogger(EmailVerificationService::class.java)

    @Transactional
    fun createVerificationToken(user: User): EmailVerificationToken {
        tokenRepository.deleteByUser(user)
        return tokenRepository.save(
            EmailVerificationToken().apply {
                token = UUID.randomUUID().toString()
                this.user = user
                expiresAt = LocalDateTime.now().plusHours(24)
                createdAt = LocalDateTime.now()
            },
        )
    }

    @Transactional
    fun verifyEmail(token: String) {
        val verificationToken =
            tokenRepository.findByToken(token).orElseThrow { TokenNotFoundException("Invalid verification token") }
        if (verificationToken.isExpired()) throw TokenExpiredException("Verification token has expired")
        val user = verificationToken.user!!
        user.emailVerified = true
        userRepository.save(user)
        tokenRepository.delete(verificationToken)
        log.info("Email verified for userId={}", user.id)
    }

    @Transactional
    fun resendVerificationEmail(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        if (user.emailVerified) throw RuntimeException("Email already verified")
        val token = createVerificationToken(user)
        emailService.sendVerificationEmail(user, token.token)
    }

    @Transactional
    fun resendVerificationEmailIfEligible(email: String) {
        userRepository.findByEmail(email).ifPresent { user ->
            if (user.authProvider == AuthProvider.local && !user.emailVerified) {
                val token = createVerificationToken(user)
                emailService.sendVerificationEmail(user, token.token)
            }
        }
    }

    @Transactional
    fun deleteExpiredTokens() = tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now())
}

@Service
class PasswordResetService(
    private val tokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenService: RefreshTokenService,
) {
    private val log = LoggerFactory.getLogger(PasswordResetService::class.java)

    @Transactional
    fun createResetToken(email: String) {
        val user = userRepository.findByEmail(email).orElse(null) ?: return
        if (user.authProvider != AuthProvider.local) {
            log.debug("Password reset requested for non-local account: {}", LogSanitizer.maskEmail(user.email))
            return
        }
        tokenRepository.deleteByUser(user)
        val token = UUID.randomUUID().toString()
        tokenRepository.save(
            PasswordResetToken().apply {
                this.token = token
                this.user = user
                expiresAt = LocalDateTime.now().plusHours(1)
                createdAt = LocalDateTime.now()
                used = false
            },
        )
        emailService.sendPasswordResetEmail(user, token)
        log.info("Password reset token issued for userId={}", user.id)
    }

    @Transactional
    fun resetPassword(
        token: String,
        newPassword: String,
    ) {
        val resetToken = tokenRepository.findByToken(token).orElseThrow { TokenNotFoundException("Invalid reset token") }
        if (!resetToken.isValid()) {
            if (resetToken.used) throw TokenExpiredException("Reset token has already been used")
            throw TokenExpiredException("Reset token has expired")
        }
        val user = resetToken.user!!
        user.password = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        refreshTokenService.deleteByUser(user)
        resetToken.used = true
        tokenRepository.save(resetToken)
        log.info("Password reset completed for userId={}", user.id)
    }

    @Transactional
    fun deleteExpiredTokens() = tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now())
}

@Service
class TurnstileService {
    private val log = LoggerFactory.getLogger(TurnstileService::class.java)
    private val restTemplate = RestTemplate()

    @Value("\${cloudflare.turnstile.secret-key}")
    private lateinit var secretKey: String

    fun validateToken(
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
                    org.springframework.http.HttpMethod.POST,
                    org.springframework.http.HttpEntity(body, headers),
                    Map::class.java,
                )
            response.body?.get("success") == true
        } catch (e: Exception) {
            log.error("Error during Turnstile token validation", e)
            false
        }
    }

    fun validateToken(token: String?): Boolean = validateToken(token, null)

    companion object {
        private const val TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify"
    }
}

@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailVerificationService: EmailVerificationService,
    private val emailService: EmailService,
    private val refreshTokenService: RefreshTokenService,
) {
    private val log = LoggerFactory.getLogger(ProfileService::class.java)

    @Transactional
    fun updateProfile(
        userId: Long,
        request: kr.devport.api.domain.auth.dto.ProfileUpdateRequest,
    ): User {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        var emailChanged = false
        if (request.email != null && request.email != user.email) {
            if (userRepository.existsByEmail(request.email)) {
                throw DuplicateEmailException("Email already exists: ${request.email}")
            }
            user.email = request.email
            user.emailVerified = false
            user.emailAddedAt = LocalDateTime.now()
            emailChanged = true
        }
        request.name?.let { user.name = it }
        request.profileImageUrl?.let { user.profileImageUrl = it }
        user.updatedAt = LocalDateTime.now()
        val saved = userRepository.save(user)
        if (emailChanged) {
            val token = emailVerificationService.createVerificationToken(saved)
            emailService.sendVerificationEmail(saved, token.token)
        }
        return saved
    }

    @Transactional
    fun changePassword(
        userId: Long,
        request: kr.devport.api.domain.auth.dto.PasswordChangeRequest,
    ) {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        if (user.authProvider != AuthProvider.local) {
            throw OAuth2AccountException("This account uses ${user.authProvider?.name} login. Password change is not available.")
        }
        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw InvalidCredentialsException("Current password is incorrect")
        }
        user.password = passwordEncoder.encode(request.newPassword)
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
        refreshTokenService.deleteByUser(user)
    }

    @Transactional
    fun removeEmail(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        if (user.authProvider == AuthProvider.local) {
            throw OAuth2AccountException("LOCAL users cannot remove email as it's required for password reset")
        }
        user.email = null
        user.emailVerified = false
        user.emailAddedAt = null
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    @Transactional
    fun updateFlair(
        userId: Long,
        request: kr.devport.api.domain.auth.dto.FlairUpdateRequest,
    ): User {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        user.flair = request.flair
        user.flairColor = request.flairColor
        user.updatedAt = LocalDateTime.now()
        return userRepository.save(user)
    }
}
