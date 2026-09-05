package kr.devport.api.domain.auth.repository

import kr.devport.api.domain.auth.entity.EmailVerificationToken
import kr.devport.api.domain.auth.entity.PasswordResetToken
import kr.devport.api.domain.auth.entity.RefreshToken
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.enums.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.Optional

// Spring Data interfaces — internal to this adapter module. The core depends on the ports in
// :auth:infrastructure; the @Repository adapters in AuthRepositoryAdapters.kt bridge the two.

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>

    fun findByUsername(username: String): Optional<User>

    fun findByAuthProviderAndProviderId(
        authProvider: AuthProvider,
        providerId: String,
    ): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}

interface RefreshTokenJpaRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    fun deleteByUser(user: User)

    fun deleteByToken(token: String)
}

interface PasswordResetTokenJpaRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>

    fun findByUser(user: User): Optional<PasswordResetToken>

    fun deleteByUser(user: User)

    fun deleteByExpiresAtBefore(dateTime: LocalDateTime)
}

interface EmailVerificationTokenJpaRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findByToken(token: String): Optional<EmailVerificationToken>

    fun findByUser(user: User): Optional<EmailVerificationToken>

    fun deleteByUser(user: User)

    fun deleteByExpiresAtBefore(dateTime: LocalDateTime)
}
