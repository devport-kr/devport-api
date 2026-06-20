package kr.devport.api.domain.auth.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import kr.devport.api.domain.auth.dto.AuthResponse
import kr.devport.api.domain.auth.dto.FlairUpdateRequest
import kr.devport.api.domain.auth.dto.ForgotPasswordRequest
import kr.devport.api.domain.auth.dto.LoginRequest
import kr.devport.api.domain.auth.dto.OAuth2ExchangeRequest
import kr.devport.api.domain.auth.dto.PasswordChangeRequest
import kr.devport.api.domain.auth.dto.ProfileUpdateRequest
import kr.devport.api.domain.auth.dto.ResendVerificationRequest
import kr.devport.api.domain.auth.dto.ResetPasswordRequest
import kr.devport.api.domain.auth.dto.SignupRequest
import kr.devport.api.domain.auth.dto.SignupResponse
import kr.devport.api.domain.auth.dto.TokenResponse
import kr.devport.api.domain.auth.dto.UserResponse
import kr.devport.api.domain.auth.service.AuthService
import kr.devport.api.domain.auth.service.EmailVerificationService
import kr.devport.api.domain.auth.service.LoginService
import kr.devport.api.domain.auth.service.PasswordResetService
import kr.devport.api.domain.auth.service.ProfileService
import kr.devport.api.domain.auth.service.RefreshTokenCookieService
import kr.devport.api.domain.auth.service.SignupService
import kr.devport.api.domain.auth.service.toUserResponse
import kr.devport.api.domain.common.exception.InvalidTokenException
import kr.devport.api.domain.common.security.CustomUserDetails
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Authentication", description = "Authentication and user management endpoints")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val signupService: SignupService,
    private val loginService: LoginService,
    private val emailVerificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService,
    private val refreshTokenCookieService: RefreshTokenCookieService,
) {
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(authService.getCurrentUser(userDetails.id))

    @PostMapping("/refresh")
    fun refreshAccessToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<TokenResponse> {
        val refreshToken = requireRefreshToken(resolveRefreshToken(request))
        val tokenResponse = authService.refreshAccessToken(refreshToken)
        refreshTokenCookieService.addRefreshTokenCookie(response, tokenResponse.refreshToken!!)
        return ResponseEntity.ok(tokenResponse.copy(refreshToken = null))
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        authService.logout(userDetails.id)
        refreshTokenCookieService.clearRefreshTokenCookie(response)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest,
    ): ResponseEntity<SignupResponse> = ResponseEntity.ok(signupService.signup(request))

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        servletResponse: HttpServletResponse,
    ): ResponseEntity<AuthResponse> {
        val authResponse = loginService.login(request)
        refreshTokenCookieService.addRefreshTokenCookie(servletResponse, authResponse.refreshToken!!)
        return ResponseEntity.ok(authResponse.copy(refreshToken = null))
    }

    @PostMapping("/oauth2/exchange")
    fun exchangeOAuth2Code(
        @Valid @RequestBody request: OAuth2ExchangeRequest,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.exchangeOAuth2Code(request.code, httpRequest)
        refreshTokenCookieService.addRefreshTokenCookie(response, tokenResponse.refreshToken!!)
        return ResponseEntity.ok(tokenResponse.copy(refreshToken = null))
    }

    @PostMapping("/verify-email")
    fun verifyEmail(
        @RequestParam token: String,
    ): ResponseEntity<Map<String, String>> {
        emailVerificationService.verifyEmail(token)
        return ResponseEntity.ok(mapOf("message" to "Email verified successfully"))
    }

    @PostMapping("/resend-verification")
    fun resendVerification(
        @Valid @RequestBody request: ResendVerificationRequest,
    ): ResponseEntity<Map<String, String>> {
        emailVerificationService.resendVerificationEmailIfEligible(request.email)
        return ResponseEntity.ok(mapOf("message" to "If the account is eligible, a verification email will be sent."))
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<Map<String, String>> {
        passwordResetService.createResetToken(request.email)
        return ResponseEntity.ok(mapOf("message" to "If the account is eligible, password reset instructions will be sent."))
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<Map<String, String>> {
        passwordResetService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.ok(mapOf("message" to "Password reset successfully"))
    }

    private fun requireRefreshToken(refreshToken: String?): String {
        if (refreshToken.isNullOrBlank()) throw InvalidTokenException("Refresh token is invalid or expired")
        return refreshToken
    }

    private fun resolveRefreshToken(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == refreshTokenCookieService.getCookieName() }?.value
}

@Tag(name = "Profile", description = "User profile management endpoints")
@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val profileService: ProfileService,
) {
    @PutMapping
    fun updateProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: ProfileUpdateRequest,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(profileService.updateProfile(userDetails.id, request).toUserResponse())

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: PasswordChangeRequest,
    ): ResponseEntity<Map<String, String>> {
        profileService.changePassword(userDetails.id, request)
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }

    @DeleteMapping("/email")
    fun removeEmail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<Map<String, String>> {
        profileService.removeEmail(userDetails.id)
        return ResponseEntity.ok(mapOf("message" to "Email removed successfully"))
    }

    @PutMapping("/flair")
    fun updateFlair(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: FlairUpdateRequest,
    ): ResponseEntity<Map<String, String>> {
        val user = profileService.updateFlair(userDetails.id, request)
        return ResponseEntity.ok(mapOf("flair" to (user.flair ?: ""), "flairColor" to (user.flairColor ?: "")))
    }
}
