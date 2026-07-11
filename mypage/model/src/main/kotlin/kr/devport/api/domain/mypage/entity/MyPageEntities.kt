package kr.devport.api.domain.mypage.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

// Cross-domain references by id: auth owns User, article owns Article. Resolve via their
// inbound ports (UserDirectory / ArticleDirectory) rather than JPA associations.

@Entity
@Table(
    name = "user_saved_articles",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "article_id"])],
    indexes = [Index(name = "idx_user_saved_articles_user_id", columnList = "user_id")],
)
class UserSavedArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(name = "article_id", nullable = false)
    var articleId: Long = 0

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}

@Entity
@Table(
    name = "user_read_history",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "article_id"])],
    indexes = [
        Index(name = "idx_user_read_history_user_id", columnList = "user_id"),
        Index(name = "idx_user_read_history_read_at", columnList = "read_at"),
    ],
)
class UserReadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(name = "article_id", nullable = false)
    var articleId: Long = 0

    @Column(nullable = false, name = "read_at")
    var readAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        readAt = LocalDateTime.now()
    }

    fun updateReadAt() {
        readAt = LocalDateTime.now()
    }
}
