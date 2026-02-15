package kr.devport.api.domain.wiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Project wiki snapshot entity with Core-6 sections and readiness metadata.
 * Mirrors the crawler WikiSnapshot contract for persistence and API consumption.
 */
@Entity
@Table(name = "project_wiki_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectWikiSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_external_id", nullable = false, unique = true, length = 255)
    private String projectExternalId;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    // Core-6 sections stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "what_section", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> whatSection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "how_section", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> howSection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "architecture_section", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> architectureSection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "activity_section", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> activitySection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "releases_section", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> releasesSection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_section", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> chatSection;

    // Readiness gates
    @Column(name = "is_data_ready", nullable = false)
    private boolean isDataReady;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hidden_sections", nullable = false, columnDefinition = "jsonb")
    private List<String> hiddenSections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "readiness_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> readinessMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Builder
    public ProjectWikiSnapshot(
            String projectExternalId,
            OffsetDateTime generatedAt,
            Map<String, Object> whatSection,
            Map<String, Object> howSection,
            Map<String, Object> architectureSection,
            Map<String, Object> activitySection,
            Map<String, Object> releasesSection,
            Map<String, Object> chatSection,
            boolean isDataReady,
            List<String> hiddenSections,
            Map<String, Object> readinessMetadata
    ) {
        this.projectExternalId = projectExternalId;
        this.generatedAt = generatedAt;
        this.whatSection = whatSection;
        this.howSection = howSection;
        this.architectureSection = architectureSection;
        this.activitySection = activitySection;
        this.releasesSection = releasesSection;
        this.chatSection = chatSection;
        this.isDataReady = isDataReady;
        this.hiddenSections = hiddenSections;
        this.readinessMetadata = readinessMetadata;
    }
}
