package kr.devport.api.domain.wiki.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "wiki_chat_messages",
    indexes = [
        Index(name = "idx_wiki_chat_messages_session_id", columnList = "session_id"),
        Index(name = "idx_wiki_chat_messages_created_at", columnList = "created_at"),
    ],
)
class WikiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", nullable = false)
    var session: WikiChatSession? = null

    @Column(name = "role", nullable = false, length = 10)
    var role: String? = null

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String? = null

    @Column(name = "is_clarification", nullable = false)
    var isClarification: Boolean = false

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}
