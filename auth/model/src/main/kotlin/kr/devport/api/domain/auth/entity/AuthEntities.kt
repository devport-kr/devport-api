package kr.devport.api.domain.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.devport.api.domain.auth.enums.AuthProvider
import kr.devport.api.domain.auth.enums.UserRole
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_username", columnList = "username"),
        Index(name = "idx_users_email", columnList = "email"),
    ],
)
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100)
    var email: String? = null

    @Column(unique = true, length = 50)
    var username: String? = null

    @Column(length = 255)
    var password: String? = null

    @Column(length = 100)
    var name: String? = null

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "auth_provider")
    var authProvider: AuthProvider? = null

    @Column(name = "provider_id", unique = true, length = 100)
    var providerId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false

    @Column(name = "email_added_at")
    var emailAddedAt: LocalDateTime? = null

    @Column(length = 30)
    var flair: String? = null

    @Column(length = 7, name = "flair_color")
    var flairColor: String? = null

    @Column(name = "agreed_terms_version", length = 20)
    var agreedTermsVersion: String? = null

    @Column(name = "agreed_at")
    var agreedAt: LocalDateTime? = null
}

@Entity
@Table(name = "refresh_tokens", indexes = [Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")])
class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(nullable = false, unique = true, length = 500)
    var token: String = ""

    @Column(nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column
    var revokedAt: LocalDateTime? = null

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isRevoked(): Boolean = revokedAt != null

    fun isValid(): Boolean = !isExpired() && !isRevoked()

    fun revoke() {
        revokedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, unique = true, length = 100)
    var token: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(nullable = false, name = "expires_at")
    var expiresAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false)
    var used: Boolean = false

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isValid(): Boolean = !used && !isExpired()
}

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, unique = true, length = 100)
    var token: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(nullable = false, name = "expires_at")
    var expiresAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
}
