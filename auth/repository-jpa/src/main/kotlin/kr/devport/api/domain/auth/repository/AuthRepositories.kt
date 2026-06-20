package kr.devport.api.domain.auth.repository

import kr.devport.api.domain.auth.entity.EmailVerificationToken
import kr.devport.api.domain.auth.entity.PasswordResetToken
import kr.devport.api.domain.auth.entity.RefreshToken
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.enums.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>

    fun findByUsername(username: String): Optional<User>

    fun findByAuthProviderAndProviderId(
        authProvider: AuthProvider,
        providerId: String,
    ): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    fun deleteByUser(user: User)

    fun deleteByToken(token: String)
}

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>

    fun findByUser(user: User): Optional<PasswordResetToken>

    fun deleteByUser(user: User)

    fun deleteByExpiresAtBefore(dateTime: LocalDateTime)
}

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findByToken(token: String): Optional<EmailVerificationToken>

    fun findByUser(user: User): Optional<EmailVerificationToken>

    fun deleteByUser(user: User)

    fun deleteByExpiresAtBefore(dateTime: LocalDateTime)
}
