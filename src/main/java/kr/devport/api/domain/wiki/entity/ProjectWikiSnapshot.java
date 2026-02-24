package kr.devport.api.domain.wiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Project wiki snapshot entity with dynamic section payload and readiness metadata.
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections", columnDefinition = "jsonb")
    private List<Map<String, Object>> sections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_counters", columnDefinition = "jsonb")
    private Map<String, Object> currentCounters;

    // Readiness gates
    @Column(name = "is_data_ready", nullable = false)
    private boolean isDataReady;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hidden_sections", columnDefinition = "jsonb")
    private List<String> hiddenSections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "readiness_metadata", columnDefinition = "jsonb")
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
            List<Map<String, Object>> sections,
            Map<String, Object> currentCounters,
            boolean isDataReady,
            List<String> hiddenSections,
            Map<String, Object> readinessMetadata
    ) {
        this.projectExternalId = projectExternalId;
        this.generatedAt = generatedAt;
        this.sections = sections;
        this.currentCounters = currentCounters;
        this.isDataReady = isDataReady;
        this.hiddenSections = hiddenSections;
        this.readinessMetadata = readinessMetadata;
    }

    public List<Map<String, Object>> getResolvedSections() {
        return sections != null ? sections : List.of();
    }

    public List<String> getHiddenSectionsOrEmpty() {
        return hiddenSections == null ? List.of() : hiddenSections;
    }

    public Map<String, Object> getResolvedCurrentCounters() {
        return currentCounters != null ? currentCounters : Map.of();
    }

    public void applyFreshnessSignal(
            OffsetDateTime generatedAt,
            Map<String, Object> currentCounters,
            Map<String, Object> readinessMetadata,
            List<String> hiddenSections
    ) {
        this.generatedAt = generatedAt == null ? OffsetDateTime.now() : generatedAt;
        this.sections = List.of();
        this.currentCounters = copyMap(currentCounters);
        this.hiddenSections = hiddenSections == null ? List.of() : new ArrayList<>(hiddenSections);
        this.readinessMetadata = copyMap(readinessMetadata);
        this.isDataReady = Boolean.TRUE.equals(this.readinessMetadata.get("passesTopStarGate"));
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        if (source == null) {
            return Map.of();
        }
        return new LinkedHashMap<>(source);
    }
}
