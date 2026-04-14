package kr.devport.api.domain.wiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "wiki_section_chunks", indexes = {
        @Index(name = "idx_wiki_section_chunks_project", columnList = "project_external_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WikiSectionChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_external_id", nullable = false, length = 255)
    private String projectExternalId;

    @Column(name = "section_id", nullable = false, length = 100)
    private String sectionId;

    @Column(name = "subsection_id", length = 100)
    private String subsectionId;

    @Column(name = "chunk_type", nullable = false, length = 20)
    private String chunkType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    private float[] embedding;

    @Column(name = "token_count")
    private int tokenCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Builder
    public WikiSectionChunk(
            Long id,
            String projectExternalId,
            String sectionId,
            String subsectionId,
            String chunkType,
            String content,
            float[] embedding,
            int tokenCount,
            Map<String, Object> metadata,
            String commitSha
    ) {
        this.id = id;
        this.projectExternalId = projectExternalId;
        this.sectionId = sectionId;
        this.subsectionId = subsectionId;
        this.chunkType = chunkType;
        this.content = content;
        this.embedding = embedding;
        this.tokenCount = tokenCount;
        this.metadata = metadata;
        this.commitSha = commitSha;
    }
}
