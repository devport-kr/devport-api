package kr.devport.api.domain.wiki.entity

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
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import java.time.LocalDateTime

@Entity
@Table(
    name = "wiki_chat_sessions",
    indexes = [
        Index(name = "idx_wiki_chat_sessions_user_id", columnList = "user_id"),
        Index(name = "idx_wiki_chat_sessions_project", columnList = "project_external_id"),
        Index(name = "idx_wiki_chat_sessions_expires_at", columnList = "expires_at"),
    ],
)
class WikiChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    var externalId: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @Column(name = "project_external_id", length = 255)
    var projectExternalId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    var sessionType: WikiChatSessionType? = null

    @Column(name = "title", length = 500)
    var title: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null

    @Column(name = "last_message_at", nullable = false)
    var lastMessageAt: LocalDateTime? = null

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
