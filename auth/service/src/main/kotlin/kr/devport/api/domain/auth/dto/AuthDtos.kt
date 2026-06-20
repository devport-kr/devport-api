package kr.devport.api.domain.auth.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kr.devport.api.domain.auth.enums.AuthProvider
import kr.devport.api.domain.auth.enums.UserRole
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponse(
    val id: Long?,
    val email: String?,
    val username: String?,
    val emailVerified: Boolean?,
    val name: String?,
    val profileImageUrl: String?,
    val authProvider: AuthProvider?,
    val role: UserRole?,
    val createdAt: LocalDateTime?,
    val lastLoginAt: LocalDateTime?,
    val flair: String?,
    val flairColor: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthResponse(
    val accessToken: String?,
    val refreshToken: String? = null,
    val tokenType: String? = "Bearer",
    val expiresIn: Long? = null,
    val user: UserResponse? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TokenResponse(
    val accessToken: String?,
    val refreshToken: String? = null,
    val tokenType: String? = "Bearer",
    val expiresIn: Long? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SignupResponse(
    val message: String?,
    val requiresEmailVerification: Boolean,
    val email: String?,
)

data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String = "",
    @field:NotBlank(message = "Password is required")
    val password: String = "",
)

data class SignupRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can only contain alphanumeric characters, dash, and underscore",
    )
    val username: String = "",
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[!@#\$%^&*(),.?\":{}|<>]).+$",
        message = "Password must contain at least one special character",
    )
    val password: String = "",
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String = "",
    @field:Size(max = 100, message = "Name must be less than 100 characters")
    val name: String? = null,
    @field:NotBlank(message = "Terms agreement is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Terms version must be in YYYY-MM-DD format")
    val agreedTermsVersion: String = "",
)

data class ProfileUpdateRequest(
    @field:Email(message = "Invalid email format")
    val email: String? = null,
    @field:Size(max = 100, message = "Name must be less than 100 characters")
    val name: String? = null,
    @field:Size(max = 500, message = "Profile image URL must be less than 500 characters")
    val profileImageUrl: String? = null,
)

data class PasswordChangeRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String = "",
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[!@#\$%^&*(),.?\":{}|<>]).+$",
        message = "Password must contain at least one special character",
    )
    val newPassword: String = "",
)

data class FlairUpdateRequest(
    @field:Size(max = 30, message = "Flair must be at most 30 characters")
    val flair: String? = null,
    @field:Pattern(
        regexp = "^#[0-9a-fA-F]{6}$",
        message = "Flair color must be a valid hex color code (e.g., #a855f7)",
    )
    val flairColor: String? = null,
)

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String = "",
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token is required")
    val token: String = "",
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[!@#\$%^&*(),.?\":{}|<>]).+$",
        message = "Password must contain at least one special character",
    )
    val newPassword: String = "",
)

data class ResendVerificationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String = "",
)

data class OAuth2ExchangeRequest(
    @field:NotBlank(message = "Code is required")
    val code: String = "",
)
