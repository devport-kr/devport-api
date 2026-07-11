package kr.devport.api.domain.auth.infrastructure

import kr.devport.api.domain.auth.entity.EmailVerificationToken
import kr.devport.api.domain.auth.entity.PasswordResetToken
import kr.devport.api.domain.auth.entity.RefreshToken
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.enums.AuthProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/**
 * Out-ports: persistence contracts owned by the auth core. The Spring Data adapters in
 * :auth:repository-jpa implement these; the core never sees JpaRepository or the repository module.
 * Optional return types are kept to match the call sites unchanged.
 */
interface UserRepository {
    fun findById(id: Long): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun findByUsername(username: String): Optional<User>

    fun findByAuthProviderAndProviderId(
        authProvider: AuthProvider,
        providerId: String,
    ): Optional<User>

    fun findAllByIdIn(ids: Collection<Long>): List<User>

    fun findAll(pageable: Pageable): Page<User>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean

    fun existsById(id: Long): Boolean

    fun save(user: User): User

    fun deleteById(id: Long)
}

interface RefreshTokenRepository {
    fun findByToken(token: String): Optional<RefreshToken>

    fun save(refreshToken: RefreshToken): RefreshToken

    fun delete(refreshToken: RefreshToken)

    fun deleteByUser(user: User)

    fun deleteByToken(token: String)
}

interface PasswordResetTokenRepository {
    fun findByToken(token: String): Optional<PasswordResetToken>

    fun findByUser(user: User): Optional<PasswordResetToken>

    fun save(token: PasswordResetToken): PasswordResetToken

    fun delete(token: PasswordResetToken)

    fun deleteByUser(user: User)

    fun deleteByExpiresAtBefore(dateTime: LocalDateTime)
}

interface EmailVerificationTokenRepository {
    fun findByToken(token: String): Optional<EmailVerificationToken>

    fun findByUser(user: User): Optional<EmailVerificationToken>

    fun save(token: EmailVerificationToken): EmailVerificationToken

    fun delete(token: EmailVerificationToken)

    fun deleteByUser(user: User)

    fun deleteByExpiresAtBefore(dateTime: LocalDateTime)
}
