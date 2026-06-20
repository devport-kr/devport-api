package kr.devport.api.domain.article.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
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
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.enums.ItemType
import kr.devport.api.domain.auth.entity.User
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "articles",
    indexes = [
        Index(name = "idx_articles_summary_ko_title", columnList = "summary_ko_title"),
        Index(name = "idx_articles_created_at_source", columnList = "created_at_source"),
    ],
)
class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, nullable = false, length = 100, name = "external_id")
    var externalId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "item_type")
    var itemType: ItemType? = null

    @Column(nullable = false, length = 100)
    var source: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: Category? = null

    @Column(nullable = false, length = 500, name = "summary_ko_title")
    var summaryKoTitle: String? = null

    @Column(columnDefinition = "TEXT", name = "summary_ko_body")
    var summaryKoBody: String? = null

    @Column(nullable = false, length = 500, name = "title_en")
    var titleEn: String? = null

    @Column(nullable = false, length = 1000)
    var url: String? = null

    @Column(nullable = false)
    var score: Int? = null

    @ElementCollection
    @CollectionTable(name = "article_tags", joinColumns = [JoinColumn(name = "article_id")])
    @Column(name = "tag")
    var tags: MutableList<String> = mutableListOf()

    @Column(nullable = false, name = "created_at_source")
    var createdAtSource: LocalDateTime? = null

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @Embedded
    var metadata: ArticleMetadata? = null

    @PrePersist
    protected fun generateExternalId() {
        if (externalId == null) {
            externalId = UUID.randomUUID().toString()
        }
    }
}

@Embeddable
class ArticleMetadata {
    @Column(name = "stars")
    var stars: Int? = null

    @Column(name = "comments")
    var comments: Int? = null

    @Column(name = "upvotes")
    var upvotes: Int? = null

    @Column(name = "read_time", length = 50)
    var readTime: String? = null

    @Column(name = "language", length = 50)
    var language: String? = null
}

@Entity
@Table(
    name = "article_comments",
    indexes = [
        Index(name = "idx_article_comments_article_id", columnList = "article_id"),
        Index(name = "idx_article_comments_user_id", columnList = "user_id"),
        Index(name = "idx_article_comments_parent_id", columnList = "parent_id"),
    ],
)
class ArticleComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, nullable = false, length = 100, name = "external_id")
    var externalId: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "article_id", nullable = false)
    var article: Article? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    var parentComment: ArticleComment? = null

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String? = null

    @Column(nullable = false)
    var deleted: Boolean = false

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID().toString()
        }
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
