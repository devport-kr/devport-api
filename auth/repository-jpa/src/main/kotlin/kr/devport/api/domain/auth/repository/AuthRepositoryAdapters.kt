package kr.devport.api.domain.auth.repository

import kr.devport.api.domain.auth.entity.EmailVerificationToken
import kr.devport.api.domain.auth.entity.PasswordResetToken
import kr.devport.api.domain.auth.entity.RefreshToken
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.enums.AuthProvider
import kr.devport.api.domain.auth.infrastructure.EmailVerificationTokenRepository
import kr.devport.api.domain.auth.infrastructure.PasswordResetTokenRepository
import kr.devport.api.domain.auth.infrastructure.RefreshTokenRepository
import kr.devport.api.domain.auth.infrastructure.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
class UserRepositoryAdapter(
    private val jpa: UserJpaRepository,
) : UserRepository {
    override fun findById(id: Long): Optional<User> = jpa.findById(id)

    override fun findByEmail(email: String): Optional<User> = jpa.findByEmail(email)

    override fun findByUsername(username: String): Optional<User> = jpa.findByUsername(username)

    override fun findByAuthProviderAndProviderId(
        authProvider: AuthProvider,
        providerId: String,
    ): Optional<User> = jpa.findByAuthProviderAndProviderId(authProvider, providerId)

    override fun findAllByIdIn(ids: Collection<Long>): List<User> = jpa.findAllById(ids)

    override fun findAll(pageable: Pageable): Page<User> = jpa.findAll(pageable)

    override fun existsByEmail(email: String): Boolean = jpa.existsByEmail(email)

    override fun existsByUsername(username: String): Boolean = jpa.existsByUsername(username)

    override fun existsById(id: Long): Boolean = jpa.existsById(id)

    override fun save(user: User): User = jpa.save(user)

    override fun deleteById(id: Long) = jpa.deleteById(id)
}

@Repository
class RefreshTokenRepositoryAdapter(
    private val jpa: RefreshTokenJpaRepository,
) : RefreshTokenRepository {
    override fun findByToken(token: String): Optional<RefreshToken> = jpa.findByToken(token)

    override fun save(refreshToken: RefreshToken): RefreshToken = jpa.save(refreshToken)

    override fun delete(refreshToken: RefreshToken) = jpa.delete(refreshToken)

    override fun deleteByUser(user: User) = jpa.deleteByUser(user)

    override fun deleteByToken(token: String) = jpa.deleteByToken(token)
}

@Repository
class PasswordResetTokenRepositoryAdapter(
    private val jpa: PasswordResetTokenJpaRepository,
) : PasswordResetTokenRepository {
    override fun findByToken(token: String): Optional<PasswordResetToken> = jpa.findByToken(token)

    override fun findByUser(user: User): Optional<PasswordResetToken> = jpa.findByUser(user)

    override fun save(token: PasswordResetToken): PasswordResetToken = jpa.save(token)

    override fun delete(token: PasswordResetToken) = jpa.delete(token)

    override fun deleteByUser(user: User) = jpa.deleteByUser(user)

    override fun deleteByExpiresAtBefore(dateTime: LocalDateTime) = jpa.deleteByExpiresAtBefore(dateTime)
}

@Repository
class EmailVerificationTokenRepositoryAdapter(
    private val jpa: EmailVerificationTokenJpaRepository,
) : EmailVerificationTokenRepository {
    override fun findByToken(token: String): Optional<EmailVerificationToken> = jpa.findByToken(token)

    override fun findByUser(user: User): Optional<EmailVerificationToken> = jpa.findByUser(user)

    override fun save(token: EmailVerificationToken): EmailVerificationToken = jpa.save(token)

    override fun delete(token: EmailVerificationToken) = jpa.delete(token)

    override fun deleteByUser(user: User) = jpa.deleteByUser(user)

    override fun deleteByExpiresAtBefore(dateTime: LocalDateTime) = jpa.deleteByExpiresAtBefore(dateTime)
}
