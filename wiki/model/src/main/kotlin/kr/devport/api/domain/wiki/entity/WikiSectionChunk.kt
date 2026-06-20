package kr.devport.api.domain.wiki.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.Array
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(
    name = "wiki_section_chunks",
    indexes = [
        Index(name = "idx_wiki_section_chunks_project", columnList = "project_external_id"),
    ],
)
class WikiSectionChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "project_external_id", nullable = false, length = 255)
    var projectExternalId: String? = null

    @Column(name = "section_id", nullable = false, length = 100)
    var sectionId: String? = null

    @Column(name = "subsection_id", length = 100)
    var subsectionId: String? = null

    @Column(name = "chunk_type", nullable = false, length = 20)
    var chunkType: String? = null

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String? = null

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    var embedding: FloatArray? = null

    @Column(name = "token_count")
    var tokenCount: Int = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: MutableMap<String, Any>? = null

    @Column(name = "commit_sha", nullable = false, length = 40)
    var commitSha: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null

    @PrePersist
    protected fun onCreate() {
        val now = OffsetDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = OffsetDateTime.now()
    }
}
